/**
 * 
 */
package org.jellyware.trinity.mdl;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.Tuple;

import org.jellyware.trinity.Entity;
import org.jellyware.trinity.ps.DeleteBuilder;
import org.jellyware.trinity.ps.QueryBuilder;
import org.jellyware.trinity.ps.QueryBuilder.Bottleneck;
import org.jellyware.trinity.ps.UpdateBuilder;

/**
 * @author Jotter
 *
 */
public class Dao<T extends Entity.Model<K>, S extends Entity.Persistence<K>, K extends Serializable>
		implements org.jellyware.trinity.Dao.Model<T, S, K> {
	private final org.jellyware.trinity.Dao.Persistence<S, K> ps;
	private final BiConsumer<S, T> deserialize;
	private final BiConsumer<T, S> serialize;
	private final Class<T> cls;

	@SuppressWarnings("unchecked")
	public Dao(org.jellyware.trinity.Dao.Persistence<S, K> ps, BiConsumer<T, S> serialize, BiConsumer<S, T> deserialize) {
		super();
		Objects.requireNonNull(ps, "Persistence Dao cannot be null");
		Objects.requireNonNull(serialize, "Serialize cannot be null");
		Objects.requireNonNull(deserialize, "Deserialize cannot be null");
		this.ps = ps;
		this.serialize = serialize;
		this.deserialize = deserialize;
		ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
		this.cls = (Class<T>) parameterizedType.getActualTypeArguments()[0];
	}

	public Dao(org.jellyware.trinity.Dao.Persistence<S, K> ps, Class<T> cls, BiConsumer<T, S> serialize,
			BiConsumer<S, T> deserialize) {
		super();
		Objects.requireNonNull(ps, "Persistence Dao cannot be null");
		Objects.requireNonNull(serialize, "Serialize cannot be null");
		Objects.requireNonNull(deserialize, "Deserialize cannot be null");
		Objects.requireNonNull(cls, "Class cannot be null");
		this.ps = ps;
		this.serialize = serialize;
		this.deserialize = deserialize;
		this.cls = cls;
	}

	@Override
	public Class<T> cls() {
		return cls;
	}

	@Override
	public Persistence<S, K> ps() {
		return ps;
	}

	@Override
	public T persist(T entity) {
		var persist = entity.getId() == null;
		CDI.current().getBeanManager().getEvent()
				.select(new Entity.Lifecycle.Dao.Literal(), new Entity.Lifecycle.Event.Literal(
						persist ? Entity.Lifecycle.PRE_PERSIST : Entity.Lifecycle.PRE_UPDATE))
				.fire(entity);
		deserialize(ps.persist(serialize(entity)), entity);
		CDI.current().getBeanManager().getEvent()
				.select(new Entity.Lifecycle.Dao.Literal(), new Entity.Lifecycle.Event.Literal(
						persist ? Entity.Lifecycle.POST_PERSIST : Entity.Lifecycle.POST_UPDATE))
				.fire(entity);
		return entity;
	}

	@Override
	public Optional<T> find(QueryBuilder<S, Tuple> qb, Bottleneck<Tuple> bottleneck) {
		return ps.find(qb, bottleneck).map(this::deserialize);
	}

	@Override
	public <U> Optional<U> find(Class<U> projection, QueryBuilder<S, U> qb, Bottleneck<U> bottleneck) {
		return ps.find(projection, qb, bottleneck);
	}

	@Override
	public Stream<T> select(QueryBuilder<S, Tuple> qb, Bottleneck<Tuple> bottleneck) {
		return ps.select(qb, bottleneck).map(this::deserialize);
	}

	@Override
	public <U> Stream<U> select(Class<U> projection, QueryBuilder<S, U> qb, Bottleneck<U> bottleneck) {
		return ps.select(projection, qb, bottleneck);
	}

	@Override
	public void delete(DeleteBuilder<S> db) {
		ps.delete(db);
	}

	@Override
	public void update(UpdateBuilder<S> ub) {
		ps.update(ub);
	}

	@Override
	public final void deserialize(S serialObject, T entity) {
		Objects.requireNonNull(entity, "Entity cannot be null");
		Objects.requireNonNull(serialObject, "Serial Object cannot be null");
		deserialize.accept(serialObject, entity);
	}

	@Override
	public final void serialize(T entity, S serialObject) {
		Objects.requireNonNull(entity, "Entity cannot be null");
		Objects.requireNonNull(serialObject, "Serial Object cannot be null");
		serialize.accept(entity, serialObject);
	}
}
