package org.jellyware.trinity;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.jellyware.trinity.ps.PredicateBuilder;

public interface Crud<K extends Serializable> {
	<T extends Entity.Model<K>, S extends Entity.Persistence<K>, U extends Entity.Serial<K>> Dao.Serial<T, S, U, K> of(
			Entity.Trio<T, S, U, K> trio);
	<S extends Entity.Model<K>> Context<S, K, Entity.Model<K>> ctx(S cls);
	EntityManager em();
	public static interface Persistence<K extends Serializable, T extends Entity.Persistence<K> & Entity.Context<K>> {
		<S extends Entity.Persistence<K>> Dao.Persistence<S, K> of(
				Class<S> cls);
		Context.Unclad<T, K> unclad(Entity<K> entity);
		<S extends Entity.Persistence<K>> Context<S, K, Entity.Persistence<K>> ctx(
				S cls);
		EntityManager em();
	}
	public static interface Model<K extends Serializable> {
		<T extends Entity.Model<K>, S extends Entity.Persistence<K>> Dao.Model<T, S, K> of(
				Entity.Trio.Persistence<T, S, K> duo);
		<S extends Entity.Model<K>> Context<S, K, Entity.Model<K>> ctx(S cls);
		EntityManager em();
	}
	public static interface Context<T extends Entity<K>, K extends Serializable, U extends Entity<K>> {
		Optional<Entity<K>> follower(String... tags);
		<V extends U> Optional<V> follower(Class<V> cls, String... tags);
		Stream<Entity<K>> followers(String... tags);
		<V extends U> Stream<V> followers(Class<V> cls, String... tags);
		void follow(Entity<K> precede, String... tags);
		void followSolely(Entity<K> precede, String... tags);
		Optional<Entity<K>> preceder(String... tags);
		<V extends U> Optional<V> preceder(Class<V> cls, String... tags);
		Stream<Entity<K>> preceders(String... tags);
		<V extends U> Stream<V> preceders(Class<V> cls, String... tags);
		void precede(Entity<K> follow, String... tags);
		void precedeSolely(Entity<K> follow, String... tags);
		public static interface Unclad<T extends Entity.Persistence<K> & Entity.Context<K>, K extends Serializable> {
			PredicateBuilder<T> follow(
					Optional<Class<? extends Entity.Model<K>>> cls,
					String... tags);
			PredicateBuilder<T> precede(
					Optional<Class<? extends Entity.Model<K>>> cls,
					String... tags);
			void precede(Entity<K> entity, String... tags);
			void follow(Entity<K> entity, String... tags);
		}
	}
}
