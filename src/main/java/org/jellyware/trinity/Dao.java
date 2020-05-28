package org.jellyware.trinity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityGraph;
import javax.persistence.Tuple;
import javax.persistence.criteria.Predicate;

import org.jellyware.beef.Beef;
import org.jellyware.trinity.ps.DeleteBuilder;
import org.jellyware.trinity.ps.PredicateBuilder;
import org.jellyware.trinity.ps.QueryBuilder;
import org.jellyware.trinity.ps.QueryBuilder.Bottleneck.GraphType;
import org.jellyware.trinity.ps.UpdateBuilder;
import org.javatuples.Pair;

public interface Dao<T extends Entity<K>, S extends Entity.Persistence<K>, K extends Serializable> {
	Class<T> cls();

	T persist(T entity);

	default Optional<T> find(K id, Optional<Pair<GraphType, EntityGraph<S>>> graph) {
		return find((cb, rt, cq) -> cq.where(cb.equal(rt, id)), tq -> {
			graph.stream().forEach(g -> tq.setHint(g.getValue0().getKey(), g.getValue1()));
			return tq;
		});
	}

	default Optional<T> find() {
		return find((cb, rt, cq) -> cq);
	};

	default Optional<T> find(K id, EntityGraph<S> graph) {
		return find(id, Optional.of(Pair.with(QueryBuilder.Bottleneck.GraphType.LOAD, graph)));
	}

	default Optional<T> find(K id) {
		return find(id, Optional.empty());
	}

	default Optional<T> find(PredicateBuilder<S> pb) {
		return find(pb, QueryBuilder.Bottleneck.free());
	}

	default Optional<T> find(PredicateBuilder<S> pb, QueryBuilder.Bottleneck<Tuple> bottleneck) {
		return find((cb, rt, qb) -> {
			var p = pb.apply(cb, rt, qb, new HashSet<>());
			return qb.where(p.toArray(new Predicate[p.size()]));
		}, bottleneck);
	}

	default Optional<T> find(QueryBuilder<S, Tuple> qb) {
		return find(qb, QueryBuilder.Bottleneck.free());
	}

	Optional<T> find(QueryBuilder<S, Tuple> qb, QueryBuilder.Bottleneck<Tuple> bottleneck);

	default <U> Optional<U> find(Class<U> projection, QueryBuilder<S, U> qb) {
		return find(projection, qb, QueryBuilder.Bottleneck.free());
	}

	<U> Optional<U> find(Class<U> projection, QueryBuilder<S, U> qb, QueryBuilder.Bottleneck<U> bottleneck);

	default Stream<T> select() {
		return select((cb, rt, cq) -> cq);
	};

	default Stream<T> select(PredicateBuilder<S> pb) {
		return select(pb, QueryBuilder.Bottleneck.free());
	}

	default Stream<T> select(PredicateBuilder<S> pb, QueryBuilder.Bottleneck<Tuple> bottleneck) {
		return select((cb, rt, qb) -> {
			var p = pb.apply(cb, rt, qb, new HashSet<>());
			return qb.where(p.toArray(new Predicate[p.size()]));
		}, bottleneck);
	}

	default Stream<T> select(QueryBuilder<S, Tuple> qb) {
		return select(qb, QueryBuilder.Bottleneck.free());
	}

	Stream<T> select(QueryBuilder<S, Tuple> qb, QueryBuilder.Bottleneck<Tuple> bottleneck);

	default <U> Stream<U> select(Class<U> projection, QueryBuilder<S, U> qb) {
		return select(projection, qb, QueryBuilder.Bottleneck.free());
	}

	<U> Stream<U> select(Class<U> projection, QueryBuilder<S, U> qb, QueryBuilder.Bottleneck<U> bottleneck);

	default void delete(K id) {
		delete((cb, rt, cd) -> cd.where(cb.equal(rt, id)));
	}

	void delete(DeleteBuilder<S> db);

	default void delete(PredicateBuilder<S> pb) {
		delete((cb, rt, cd) -> {
			var p = pb.apply(cb, rt, cd, new HashSet<>());
			return cd.where(p.toArray(new Predicate[p.size()]));
		});
	}

	void update(UpdateBuilder<S> ub);

	default void update(UpdateBuilder<S> ub, PredicateBuilder<S> pb) {
		update((cb, rt, cd) -> {
			var p = pb.apply(cb, rt, cd, new HashSet<>());
			return ub.apply(cb, rt, cd).where(p.toArray(new Predicate[p.size()]));
		});
	}

	public static interface Persistence<S extends Entity.Persistence<K>, K extends Serializable> extends Dao<S, S, K> {
		@Override
		Class<S> cls();
	}

	public static interface Model<T extends Entity.Model<K>, S extends Entity.Persistence<K>, K extends Serializable>
			extends Dao<T, S, K> {
		@Override
		Class<T> cls();

		Dao.Persistence<S, K> ps();

		default S serialize(T entity) {
			try {
				var constructor = ps().cls().getDeclaredConstructor();
				constructor.setAccessible(true);
				var s = constructor.newInstance();
				serialize(entity, s);
				return s;
			} catch (InvocationTargetException e) {
				throw Beef
						.of(TrinityException.class).following(e.getCause()).as(b -> b.when("Serializing")
								.detail("Could not selize to SerialEntity").may("Contact application administrator"))
						.build();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
					| SecurityException e) {
				throw Beef
						.of(TrinityException.class).following(e).as(b -> b.when("Serializing")
								.detail("Could not selize to SerialEntity").may("Contact application administrator"))
						.build();
			}
		}

		default T deserialize(S serialObject) {
			try {
				var constructor = cls().getDeclaredConstructor();
				constructor.setAccessible(true);
				var t = constructor.newInstance();
				deserialize(serialObject, t);
				return t;
			} catch (InvocationTargetException e) {
				throw Beef.uncheck(e);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
					| SecurityException e) {
				throw Beef.of(TrinityException.class).following(e).as(b -> b.when("Serializing")
						.detail("Could not deserialize to ModelEntity").may("Contact application administrator"))
						.build();
			}
		}

		void deserialize(S serialObject, T entity);

		void serialize(T entity, S serialObject);
	}

	public static interface Serial<T extends Entity.Model<K>, S extends Entity.Persistence<K>, U extends Entity.Serial<K>, K extends Serializable>
			extends Dao<T, S, K> {
		@Override
		Class<T> cls();

		Class<U> ser();

		Dao.Model<T, S, K> mdl();

		default U serialize(T entity) {
			try {
				var constructor = ser().getDeclaredConstructor();
				constructor.setAccessible(true);
				var s = constructor.newInstance();
				serialize(entity, s);
				return s;
			} catch (InvocationTargetException e) {
				throw Beef.uncheck(e);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
					| SecurityException e) {
				throw Beef
						.of(TrinityException.class).following(e).as(b -> b.when("Serializing")
								.detail("Could not selize to SerialEntity").may("Contact application administrator"))
						.build();
			}
		}

		default T deserialize(U serialObject) {
			try {
				var constructor = cls().getDeclaredConstructor();
				constructor.setAccessible(true);
				var t = constructor.newInstance();
				deserialize(serialObject, t);
				return t;
			} catch (InvocationTargetException e) {
				throw Beef.uncheck(e);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException
					| SecurityException e) {
				throw Beef.of(TrinityException.class).following(e).as(b -> b.when("Serializing")
						.detail("Could not deserialize to ModelEntity").may("Contact application administrator"))
						.build();
			}
		}

		void deserialize(U serialObject, T entity);

		void serialize(T entity, U serialObject);
	}
}
