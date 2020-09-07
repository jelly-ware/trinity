package org.jellyware.trinity;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Qualifier;

import org.jellyware.beef.Beef;
import org.jellyware.trinity.Entity.Model;

public interface Creation<K extends Serializable> {
	<U extends Entity.Model<K> & Creatable> U create(U entity);

	default <U extends Model<K> & Creatable> U entity(Class<U> cls, String key) {
		return find(cls, key).orElseThrow(() -> Beef.of(TrinityException.class)
				.as(b -> b.when("Fetching creational entity for " + cls).detail("Couldn't find key - " + key)).build());
	}

	default <U extends Entity.Model<K> & Creatable> U entity(Class<U> cls, String key, Supplier<U> supplier) {
		try {
			return entity(cls, key);
		} catch (Exception e) {
			try {
				return create(supplier.get());
			} catch (Exception e1) {
				throw e1;
			}
		}
	}

	default <U extends Model<K> & Creatable> Stream<U> entities(Class<U> cls, String... keys) {
		return select(cls, keys);
	}

	@SuppressWarnings("unchecked")
	default <U extends Model<K> & Creatable> K id(Class<U> cls, String key) {
		return find((Class<K>) ((ParameterizedType) this.getClass().getGenericInterfaces()[0])
				.getActualTypeArguments()[0], cls, key).orElseThrow(() -> Beef.of(TrinityException.class)
						.as(b -> b.when("Fetching creational entity for " + cls).detail("Couldn't find key - " + key))
						.build());
	}

	@SuppressWarnings("unchecked")
	default <U extends Model<K> & Creatable> Stream<K> ids(Class<U> cls, String... keys) {
		return select(
				(Class<K>) ((ParameterizedType) this.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0],
				cls, keys);
	}

	<T, U extends Model<K> & Creatable> Stream<T> select(Class<T> projection, Class<U> cls, String... keys);

	default <T extends Model<K> & Creatable> Stream<T> select(Class<T> cls, String... keys) {
		return select(cls, cls, keys);
	}

	<T, U extends Model<K> & Creatable> Optional<T> find(Class<T> projection, Class<U> cls, String key);

	default <T extends Model<K> & Creatable> Optional<T> find(Class<T> cls, String key) {
		return find(cls, cls, key);
	}

	Crud.Model<K> crud();

	public static interface Discrimination<V extends Entity.Model<K>, K extends Serializable> {
		<U extends Entity.Model<K> & Discriminatory<V, K>> U create(U entity);

		default <U extends Model<K> & Discriminatory<V, K>> U entity(Class<U> cls, String key) {
			return find(cls, key).orElseThrow(() -> Beef.of(TrinityException.class)
					.as(b -> b.when("Fetching creational entity for " + cls).detail("Couldn't find key - " + key))
					.build());
		}

		default <U extends Entity.Model<K> & Discriminatory<V, K>> U entity(Class<U> cls, String key,
				Supplier<U> supplier) {
			try {
				return entity(cls, key);
			} catch (Exception e) {
				try {
					return create(supplier.get());
				} catch (Exception e1) {
					throw e1;
				}
			}
		}

		default <U extends Model<K> & Discriminatory<V, K>> Stream<U> entities(Class<U> cls, String... keys) {
			return select(cls, keys);
		}

		@SuppressWarnings("unchecked")
		default <U extends Model<K> & Discriminatory<V, K>> K id(Class<U> cls, String key) {
			return find((Class<K>) ((ParameterizedType) this.getClass().getGenericInterfaces()[0])
					.getActualTypeArguments()[1], cls, key).orElseThrow(() -> Beef.of(TrinityException.class).as(
							b -> b.when("Fetching creational entity for " + cls).detail("Couldn't find key - " + key))
							.build());
		}

		@SuppressWarnings("unchecked")
		default <U extends Model<K> & Discriminatory<V, K>> Stream<K> ids(Class<U> cls, String... keys) {
			return select((Class<K>) ((ParameterizedType) this.getClass().getGenericInterfaces()[0])
					.getActualTypeArguments()[1], cls, keys);
		}

		<T, U extends Model<K> & Discriminatory<V, K>> Stream<T> select(Class<T> projection, Class<U> cls,
				String... keys);

		default <T extends Model<K> & Discriminatory<V, K>> Stream<T> select(Class<T> cls, String... keys) {
			return select(cls, cls, keys);
		}

		<T, U extends Model<K> & Discriminatory<V, K>> Optional<T> find(Class<T> projection, Class<U> cls, String key);

		default <T extends Model<K> & Discriminatory<V, K>> Optional<T> find(Class<T> cls, String key) {
			return find(cls, cls, key);
		}

		Crud.Model<K> crud();

		V discriminator();

		Creation<K> creation();

		<T extends Entity.Model<K>> Discrimination<T, K> of(T discriminator);

		public static interface Creator<K extends Serializable> {
			<T extends Entity.Model<K>> Discrimination<T, K> of(T discriminator);
		}

		public static interface Discriminatory<T extends Model<K>, K extends Serializable> extends Creatable {
			void discriminate(T entity);
		}
	}

	@javax.inject.Qualifier
	@Retention(RUNTIME)
	@Target({ ElementType.TYPE, ElementType.PARAMETER })
	public static @interface Qualifier {
	}

	@Documented
	@Target({ ElementType.METHOD })
	@Retention(RUNTIME)
	public static @interface Recipe {
		boolean autoCreate() default true;

		boolean constrain() default true;
	}

	public static interface Creatable {
		String key();
	}
}
