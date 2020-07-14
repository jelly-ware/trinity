/**
 * 
 */
package org.jellyware.trinity;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.jellyware.beef.Beef;

/**
 * @author Jotter
 * @param <K>
 *
 */
public interface Entity<K extends Serializable> {
	K getId();

	void setId(K id);

	boolean isConstrained();

	void setConstrained(boolean constrained);

	String getDisplay();

	Set<String> getTags();

	void setTags(Set<String> tags);

	public static interface Descriptor extends Creation.Creatable {
		String getCodeName();

		void setCodeName(String name);

		String getName();

		void setName(String name);

		String getDescription();

		void setDescription(String description);

		@Override
		default String key() {
			return getCodeName();
		}
	}

	@FunctionalInterface
	public static interface Hierarchical<T extends Entity<K> & Hierarchical<T, K>, K extends Serializable> {
		T getParent();

		@SuppressWarnings("unchecked")
		default Optional<T> node(Predicate<T> predicate) {
			var node = (T) this;
			do {
				if (predicate.test(node))
					return Optional.of(node);
				node = node.getParent();
			} while (node != null);
			return Optional.empty();
		}

		default boolean hasNode(Predicate<T> predicate) {
			return node(predicate).isPresent();
		}

		default T root() {
			return node(n -> n.getParent() == null).orElse(null);
		}

		public static interface Helper<K extends Serializable> {
			Stream<K> streamImmediateChildren(Class<? extends Entity<K>> cls, K id);

			default Stream<K> streamImmediateChildren(Entity<K> entity) {
				return streamImmediateChildren((Class<? extends Entity<K>>) entity.getClass(), entity.getId());
			}

			Set<K> obtainChildren(Class<? extends Entity<K>> cls, K id);

			default Set<K> obtainChildren(Entity<K> cls) {
				return obtainChildren((Class<? extends Entity<K>>) cls.getClass(), cls.getId());
			}
		}
	}

	public static interface State<T extends Enum<T>> {
		T getStatus();

		void setStatus(T status);

		<S extends State<T>> S hibernate();

		<S extends State<T>> S await();

		<S extends State<T>> S approve();

		<S extends State<T>> S deny();

		<S extends State<T>> S deactivate();
	}

	public static interface Context<K extends Serializable> {
		Class<? extends Model<K>> getPrecede();

		void setPrecede(Class<? extends Model<K>> cls);

		K getPrecedeId();

		void setPrecedeId(K id);

		Class<? extends Model<K>> getFollow();

		void setFollow(Class<? extends Model<K>> cls);

		K getFollowId();

		void setFollowId(K id);
	}

	public static interface Model<K extends Serializable> extends Entity<K> {
		<T extends Model<K>> T duplicate();
	}

	public static interface Persistence<K extends Serializable> extends Entity<K> {
		@Retention(RUNTIME)
		@Target({ METHOD })
		public static @interface Formula {
			String value();
		}
	}

	public static interface Serial<K extends Serializable> extends Entity<K> {
	}

	public static interface Trio<T extends Entity.Model<K>, S extends Entity.Persistence<K>, U extends Entity.Serial<K>, K extends Serializable> {
		Class<T> mdl();

		Class<S> ps();

		Class<U> ser();

		Persistence<T, S, K> persistence();

		Serial<T, U, K> serial();

		public static <T extends Entity.Model<K>, S extends Entity.Persistence<K>, U extends Entity.Serial<K>, K extends Serializable> Trio<T, S, U, K> of(
				Class<T> model, Class<S> persistence, Class<U> serial) {
			return new Trio<>() {
				@Override
				public Class<T> mdl() {
					return model;
				}

				@Override
				public Class<S> ps() {
					return persistence;
				}

				@Override
				public Class<U> ser() {
					return serial;
				}

				@Override
				public Persistence<T, S, K> persistence() {
					return Persistence.of(model, persistence);
				}

				@Override
				public Serial<T, U, K> serial() {
					return Serial.of(model, serial);
				}
			};
		}

		public static interface Pouch<K extends Serializable> {
			Stream<Trio<? extends Model<K>, ? extends org.jellyware.trinity.Entity.Persistence<K>, ? extends org.jellyware.trinity.Entity.Serial<K>, K>> trios();

			default <T extends Entity.Model<K>> Trio<T, ? extends Entity.Persistence<K>, ? extends Entity.Serial<K>, K> mdl(
					Class<T> mdl) {
				return trios().filter(trio -> trio.mdl().equals(mdl)).findAny()
						.map(trio -> Trio.of(mdl, trio.ps(), trio.ser()))
						.orElseThrow(() -> Beef.of(TrinityException.class)
								.as(b -> b.when("Fetching trio.").detail("Coulnd't find trio for model entity " + mdl))
								.build());
			}

			default <U extends Entity.Serial<K>> Trio<? extends Entity.Model<K>, ? extends Entity.Persistence<K>, U, K> ser(
					Class<U> ser) {
				return trios().filter(trio -> trio.ser().equals(ser)).findAny()
						.map(trio -> Trio.of(trio.mdl(), trio.ps(), ser))
						.orElseThrow(() -> Beef.of(TrinityException.class)
								.as(b -> b.when("Fetching trio.").detail("Coulnd't find trio for serial entity " + ser))
								.build());
			}

			default <S extends Entity.Persistence<K>> Trio<? extends Entity.Model<K>, S, ? extends Entity.Serial<K>, K> ps(
					Class<S> ps) {
				return trios().filter(trio -> trio.ps().equals(ps)).findAny()
						.map(trio -> Trio.of(trio.mdl(), ps, trio.ser()))
						.orElseThrow(() -> Beef.of(TrinityException.class).as(
								b -> b.when("Fetching trio.").detail("Coulnd't find trio for persistence entity " + ps))
								.build());
			}

			@SuppressWarnings("unchecked")
			default Trio<? extends Entity.Model<K>, ? extends Entity.Persistence<K>, ? extends Entity.Serial<K>, K> of(
					Class<? extends Entity<K>> cls) {
				if (Entity.Model.class.isAssignableFrom(cls)) {
					return mdl((Class<? extends Entity.Model<K>>) cls);
				} else if (Entity.Serial.class.isAssignableFrom(cls)) {
					return ser((Class<? extends Entity.Serial<K>>) cls);
				} else if (Entity.Persistence.class.isAssignableFrom(cls)) {
					return ps((Class<? extends Entity.Persistence<K>>) cls);
				} else {
					throw Beef.of(TrinityException.class)
							.as(b -> b.when("Fetching trio.").detail("Coulnd't find trio for serial entity " + cls)
									.may("Contact application administrator"))
							.build();
				}
			}
		}

		public static interface Persistence<T extends Entity.Model<K>, S extends Entity.Persistence<K>, K extends Serializable> {
			Class<T> mdl();

			Class<S> ps();

			default <U extends Entity.Serial<K>> Trio<T, S, U, K> trio(Class<U> ser) {
				return Trio.of(mdl(), ps(), ser);
			};

			public static <T extends Entity.Model<K>, S extends Entity.Persistence<K>, K extends Serializable> Persistence<T, S, K> of(
					Class<T> mdl, Class<S> ps) {
				return new Persistence<T, S, K>() {
					@Override
					public Class<T> mdl() {
						return mdl;
					}

					@Override
					public Class<S> ps() {
						return ps;
					}
				};
			}
		}

		public static interface Serial<T extends Entity.Model<K>, S extends Entity.Serial<K>, K extends Serializable> {
			Class<T> mdl();

			Class<S> ser();

			default <U extends Entity.Persistence<K>> Trio<T, U, S, K> trio(Class<U> ps) {
				return Trio.of(mdl(), ps, ser());
			};

			public static <T extends Entity.Model<K>, S extends Entity.Serial<K>, K extends Serializable> Serial<T, S, K> of(
					Class<T> mdl, Class<S> ser) {
				return new Serial<T, S, K>() {
					@Override
					public Class<T> mdl() {
						return mdl;
					}

					@Override
					public Class<S> ser() {
						return ser;
					}
				};
			}
		}
	}

	public enum Lifecycle {
		POST_PERSIST, POST_REMOVE, POST_UPDATE, PRE_PERSIST, PRE_REMOVE, PRE_UPDATE, POST_LOAD;

		@Qualifier
		@Retention(RUNTIME)
		@Target({ FIELD, PARAMETER, METHOD, TYPE })
		public static @interface Event {
			Lifecycle value();

			@SuppressWarnings("serial")
			class Literal extends AnnotationLiteral<Event> implements Event {
				private final Lifecycle lifecycle;

				public Literal(Lifecycle lifecycle) {
					super();
					this.lifecycle = lifecycle;
				}

				@Override
				public Lifecycle value() {
					return lifecycle;
				}
			}
		}

		@Qualifier
		@Retention(RUNTIME)
		@Target({ FIELD, PARAMETER, METHOD, TYPE })
		public static @interface Crud {
			@SuppressWarnings("serial")
			class Literal extends AnnotationLiteral<Crud> implements Crud {
				public Literal() {
					super();
				}
			}
		}

		@Qualifier
		@Retention(RUNTIME)
		@Target({ FIELD, PARAMETER, METHOD, TYPE })
		public static @interface Dao {
			@SuppressWarnings("serial")
			class Literal extends AnnotationLiteral<Dao> implements Dao {
				public Literal() {
					super();
				}
			}

			@Qualifier
			@Retention(RUNTIME)
			@Target({ FIELD, PARAMETER, METHOD, TYPE })
			public static @interface Ps {
				@SuppressWarnings("serial")
				class Literal extends AnnotationLiteral<Ps> implements Ps {
					public Literal() {
						super();
					}
				}
			}
		}

		@Qualifier
		@Retention(RUNTIME)
		@Target({ FIELD, PARAMETER, METHOD, TYPE })
		public static @interface JPA {
			@SuppressWarnings("serial")
			class Literal extends AnnotationLiteral<JPA> implements JPA {
				public Literal() {
					super();
				}
			}
		}
	}
}
