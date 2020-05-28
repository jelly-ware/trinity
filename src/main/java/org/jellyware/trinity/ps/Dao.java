package org.jellyware.trinity.ps;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.jellyware.beef.Beef;
import org.jellyware.toolkit.Reflect;
import org.jellyware.trinity.Entity;
import org.jellyware.trinity.TrinityException;
import org.jellyware.trinity.ps.QueryBuilder.Bottleneck;
import static org.reflections8.ReflectionUtils.*;

public class Dao<T extends Entity.Persistence<K>, K extends Serializable>
		implements org.jellyware.trinity.Dao.Persistence<T, K> {
	protected final EntityManager em;
	private final Class<T> cls;

	// For inheritance
	@SuppressWarnings("unchecked")
	public Dao(EntityManager em) {
		this.em = em;
		ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
		this.cls = (Class<T>) parameterizedType.getActualTypeArguments()[0];
	}

	// For instantiation
	public Dao(EntityManager em, Class<T> entityClass) {
		super();
		this.em = em;
		this.cls = entityClass;
	}

	@Override
	public Class<T> cls() {
		return cls;
	}

	@Override
	public T persist(T entity) {
		var persist = entity.getId() == null;
		CDI.current().getBeanManager().getEvent()
				.select(new Entity.Lifecycle.Dao.Ps.Literal(), new Entity.Lifecycle.Event.Literal(
						persist ? Entity.Lifecycle.PRE_PERSIST : Entity.Lifecycle.PRE_UPDATE))
				.fire(entity);
		var rtn = em.merge(entity);
		CDI.current().getBeanManager().getEvent()
				.select(new Entity.Lifecycle.Dao.Ps.Literal(), new Entity.Lifecycle.Event.Literal(
						persist ? Entity.Lifecycle.POST_PERSIST : Entity.Lifecycle.POST_UPDATE))
				.fire(rtn);
		return rtn;
	}

	private List<Selection<?>> selections(CriteriaBuilder cb, Root<T> rt, CommonAbstractCriteria c) {
		var rtn = new ArrayList<Selection<?>>();
		getAllFields(cls, withModifier(Modifier.FINAL).negate(), withModifier(Modifier.STATIC).negate(),
				withTypeAssignableTo(Collection.class).and(withAnnotation(OneToMany.class)).negate(),
				withTypeAssignableTo(Map.class).negate()).forEach(f -> {
					var t = f.getAnnotation(Transient.class);
					if (t != null || Modifier.isTransient(f.getModifiers())) {
						getAllMethods(cls, withAnnotation(Entity.Persistence.Formula.class),
								withReturnTypeAssignableTo(Selection.class), withParametersAssignableFrom(
										CriteriaBuilder.class, From.class, CommonAbstractCriteria.class)).stream()
												.findFirst().ifPresent(m -> {
													var formula = m
															.getDeclaredAnnotation(Entity.Persistence.Formula.class);
													if (formula != null)
														if (formula.value().equals(f.getName()))
															rtn.add(Reflect.<Object, Selection<String>>method(m)
																	.executeStatic(cb, rt, c).alias(f.getName()));
												});
					} else if (Entity.Persistence.class.isAssignableFrom(f.getType()))
						rtn.add(rt.join(f.getName(), JoinType.LEFT).alias(f.getName()));
					else
						rtn.add(rt.get(f.getName()).alias(f.getName()));
				});
		return rtn;
	}

	private T mapper(Tuple tuple) {
		try {
			T entity = Reflect.constructor(cls.getDeclaredConstructor()).execute();
			for (var f : getAllFields(cls, withModifier(Modifier.FINAL).negate(),
					withModifier(Modifier.STATIC).negate(),
					withTypeAssignableTo(Collection.class).and(withAnnotation(OneToMany.class)).negate(),
					withTypeAssignableTo(Map.class).negate())) {
				Object val = null;
				try {
					val = tuple.get(f.getName());
				} catch (IllegalArgumentException e) {
				}
				if (val != null)
					try {
						f.setAccessible(true);
						f.set(entity, val);
					} catch (Exception e) {
						throw Beef.of(TrinityException.class).following(e).as(b -> b.when("Mapping tuple")
								.detail("Couldn't set field").may("Contact application administrator")).build();
					}
			}
			return entity;
		} catch (NoSuchMethodException | SecurityException e) {
			throw Beef.of(e).build();
		}
	}

	private <U> TypedQuery<U> createTypedQuery(Class<U> projection, QueryBuilder<T, U> qb,
			QueryBuilder.Bottleneck<U> bottleneck) {
		CriteriaQuery<U> cq = em.getCriteriaBuilder().createQuery(projection);
		Root<T> root = cq.from(cls);
		TypedQuery<U> tq = em.createQuery(qb.apply(em.getCriteriaBuilder(), root, cq));
		return bottleneck.apply(tq);
	}

	@Override
	public <U> Optional<U> find(Class<U> projection, QueryBuilder<T, U> qb, Bottleneck<U> bottleneck) {
		try {
			return Optional.ofNullable(createTypedQuery(projection, qb, bottleneck).getSingleResult());
		} catch (NoResultException e) {
			return Optional.empty();
		}
	}

	@Override
	public Optional<T> find(QueryBuilder<T, Tuple> qb, Bottleneck<Tuple> bottleneck) {
		return find(Tuple.class, (cb, rt, cq) -> qb.apply(cb, rt, cq).multiselect(selections(cb, rt, cq)), bottleneck)
				.map(this::mapper);
	}

	@Override
	public <U> Stream<U> select(Class<U> projection, QueryBuilder<T, U> qb, Bottleneck<U> bottleneck) {
		return createTypedQuery(projection, qb, bottleneck).getResultStream();
	}

	@Override
	public Stream<T> select(QueryBuilder<T, Tuple> qb, Bottleneck<Tuple> bottleneck) {
		return select(Tuple.class, (cb, rt, cq) -> qb.apply(cb, rt, cq).multiselect(selections(cb, rt, cq)), bottleneck)
				.map(this::mapper);
	}

	@Override
	public void delete(DeleteBuilder<T> db) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaDelete<T> delete = cb.createCriteriaDelete(cls);
		Root<T> root = delete.from(cls);
		db.apply(cb, root, delete);
		em.createQuery(delete).executeUpdate();
	}

	@Override
	public void update(UpdateBuilder<T> ub) {
		CriteriaUpdate<T> update = em.getCriteriaBuilder().createCriteriaUpdate(cls);
		Root<T> root = update.from(cls);
		ub.apply(em.getCriteriaBuilder(), root, update);
		em.createQuery(update).executeUpdate();
	}
}
