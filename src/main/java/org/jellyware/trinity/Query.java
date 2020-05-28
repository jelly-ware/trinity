package org.jellyware.trinity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonString;
import javax.json.JsonValue;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.jellyware.beef.Beef;
import org.jellyware.toolkit.Enumeration;
import org.jellyware.toolkit.Json;
import org.jellyware.toolkit.Reflect;
import org.jellyware.trinity.Entity.Persistence;
import org.jellyware.trinity.ps.OrderBuilder;
import org.jellyware.trinity.ps.PredicateBuilder;
import org.jellyware.trinity.ps.QueryBuilder;
import org.javatuples.Pair;

import static org.reflections8.ReflectionUtils.*;

public class Query {
	public static final String X = "x";
	public static final String Y = "y";

	public static <T extends Entity.Model<? extends Serializable>, S extends Entity.Persistence<? extends Serializable>, W> Optional<W> find(
			Class<W> projection, Dao.Model<T, S, ? extends Serializable> dao, PredicateBuilder<S> pb,
			Query.Parameters params, Function<T, W> mapper) {
		return dao.find((cb, rt, qb) -> {
			var p = where(dao.ps().cls(), params, pb).apply(cb, rt, qb, new HashSet<>());
			return qb.where(p.toArray(new Predicate[p.size()]));
		}).map(mapper);
	}

	public static <T extends Entity.Model<? extends Serializable>, S extends Entity.Persistence<? extends Serializable>> Optional<T> find(
			Dao.Model<T, S, ? extends Serializable> dao, Query.Parameters params) {
		return find(dao, (cb, rt, c, p) -> p, params);
	}

	public static <T extends Entity.Model<? extends Serializable>, S extends Entity.Persistence<? extends Serializable>> Optional<T> find(
			Dao.Model<T, S, ? extends Serializable> dao, PredicateBuilder<S> pb, Query.Parameters params) {
		return find(dao.cls(), dao, pb, params, Function.identity());
	}

	public static <T extends Entity.Model<? extends Serializable>, S extends Entity.Persistence<? extends Serializable>> Query.Response<T> select(
			Dao.Model<T, S, ? extends Serializable> dao, Query.Parameters.Pagination params) {
		return select(dao, (cb, rt, c, p) -> p, params);
	}

	public static <T extends Entity.Model<? extends Serializable>, S extends Entity.Persistence<? extends Serializable>> Query.Response<T> select(
			Dao.Model<T, S, ? extends Serializable> dao, PredicateBuilder<S> pb, Query.Parameters.Pagination params) {
		return select(dao.cls(), dao, pb, params, Function.identity());
	}

	@SuppressWarnings("unchecked")
	public static <T extends Entity.Model<? extends Serializable>, S extends Entity.Persistence<? extends Serializable>, W> Query.Response<W> select(
			Class<W> projection, Dao.Model<T, S, ? extends Serializable> dao, PredicateBuilder<S> pb,
			Query.Parameters.Pagination params, Function<T, W> mapper) {
		var resp = new Query.Response<W>();

		// paginate
		var count = dao.find(Long.class, (cb, rt, qb) -> {
			var p = where(dao.ps().cls(), params, pb).apply(cb, rt, qb, new HashSet<>());
			return qb.select(cb.count(rt)).where(p.toArray(new Predicate[p.size()]));
		}).orElse(0L);
		resp.setData(dao.select((cb, rt, qb) -> {
			var p = where(dao.ps().cls(), params, pb).apply(cb, rt, qb, new HashSet<>());
			var o = order(params).apply(cb, (Root<Persistence<? extends Serializable>>) rt, new HashSet<>());
			return qb.where(p.toArray(new Predicate[p.size()])).orderBy(o.toArray(new Order[o.size()]));
		}, paginate(params.getPageNumber(), params.getPageSize())).map(mapper).collect(Collectors.toList()));
		var meta = new Query.Response.Meta();
		meta.setLastPageNumber((int) Math.ceil((double) count / params.getPageSize()));
		meta.setTotalCount(count);
		meta.setPageNumber(params.getPageNumber());
		meta.setPageSize(params.getPageSize());
		resp.setMeta(meta);

		return resp;
	}

	public static <T extends Entity.Persistence<? extends Serializable>> PredicateBuilder<T> where(Class<T> cls,
			Parameters parameters, PredicateBuilder<T> pb) {
		return (cb, rt, c, p) -> {
			pb.apply(cb, rt, c, p);
			for (var predicate : parameters.getWhere())
				p.add(parse(cls, predicate, cb, rt, c));
			return p;
		};
	}

	public static <T extends Entity.Persistence<? extends Serializable>, U> Pair<Field, Expression<U>> parse(
			Class<T> cls, String expression, CriteriaBuilder cb, Root<T> rt, CommonAbstractCriteria c) {
		var field = getAllFields(cls, withModifier(Modifier.FINAL).negate(), withModifier(Modifier.STATIC).negate(),
				withTypeAssignableTo(Collection.class).and(withAnnotation(OneToMany.class)).negate(),
				// withTypeAssignableTo(Map.class).negate(),
				withName(expression))
						.stream().findAny()
						.orElseThrow(() -> Beef.of(TrinityException.class).as(
								b -> b.when("Parsing expression").detail("Couldn't find suitable field: " + expression))
								.build());
		var t = field.getAnnotation(Transient.class);
		if (t != null || Modifier.isTransient(field.getModifiers())) {
			var m = getAllMethods(cls, withAnnotation(Entity.Persistence.Formula.class),
					withReturnTypeAssignableTo(Selection.class),
					withParametersAssignableFrom(CriteriaBuilder.class, From.class, CommonAbstractCriteria.class))
							.stream().filter(method -> {
								var formula = method.getDeclaredAnnotation(Entity.Persistence.Formula.class);
								if (formula != null)
									return formula.value().equals(field.getName());
								return false;
							}).findFirst().orElseThrow(
									() -> Beef.of(TrinityException.class)
											.as(b -> b.when("Parsing expression")
													.detail("Couldn't find suitable " + expression + " method."))
											.build());
			return Pair.with(field, Reflect.<Object, Expression<U>>method(m).execute(null, cb, rt, c));
		}
		// return Pair.with(field, Reflect.<Object,
		// Expression<U>>method(m).execute(null, cb,
		// rt.join(field.getName(), JoinType.LEFT), c));
		return Pair.with(field, rt.get(field.getName()));
	}

	public static Object parse(JsonValue value, Class<?> cls) {
		java.util.function.Predicate<java.lang.reflect.Type> ip = t -> {
			if (t instanceof ParameterizedType)
				return ((ParameterizedType) t).getRawType() == Entity.Persistence.class;
			else if (t instanceof Class)
				return t == Entity.Persistence.class;
			else
				return false;
		};
		var ps = Reflect.genericSuperclass(cls, c -> {
			if (c instanceof ParameterizedType)
				return Reflect.genericInterface((Class<?>) ((ParameterizedType) c).getRawType(), ip).isPresent();
			else if (c instanceof Class)
				return Reflect.genericInterface((Class<?>) c, ip).isPresent();
			else
				return false;
		});
		return Json.parse(value, ps.isPresent() ? ps.map(
				c -> ((ParameterizedType) Reflect.genericInterface((Class<?>) c, ip).get()).getActualTypeArguments()[0])
				.get() : cls);
	}

	public static <T extends Entity.Persistence<? extends Serializable>> Predicate parse(Class<T> cls,
			Query.Parameters.Predicate serial, CriteriaBuilder cb, Root<T> rt, CommonAbstractCriteria c) {
		Predicate predicate;
		// find x
		Pair<Field, ? extends Expression<?>> x;
		switch (serial.type) {
			case EQUAL:
				x = Query.parse(cls, ((JsonString) serial.getParams().get(X)).getString(), cb, rt, c);
				predicate = cb.equal(x.getValue1(), parse(serial.getParams().get(Y), x.getValue0().getType()));
				break;
			case LIKE:
				x = Query.parse(cls, ((JsonString) serial.getParams().get(X)).getString(), cb, rt, c);
				if (Enum.class.isAssignableFrom(x.getValue0().getType())) {
					@SuppressWarnings("unchecked")
					var arr = Stream.of(Enumeration.values((Class<? extends Enum>) x.getValue0().getType()))
							.filter(e -> e.toString().toUpperCase()
									.contains((serial.getParams().get(Y).getValueType() == JsonValue.ValueType.STRING
											? ((JsonString) serial.getParams().get(Y)).getString()
											: serial.getParams().get(Y).toString()).toUpperCase()))
							.toArray();
					if (arr.length > 0)
						predicate = x.getValue1().in(arr);
					else
						predicate = cb.isTrue(cb.literal(false));
				} else
					predicate = cb.like(cb.upper(x.getValue1().as(String.class)),
							'%' + ((serial.getParams().get(Y).getValueType() == JsonValue.ValueType.STRING)
									? ((JsonString) serial.getParams().get(Y)).getString()
									: serial.getParams().get(Y).toString()).toUpperCase() + '%');
				break;
			case AND:
				if (serial.getParams().containsKey(X))
					predicate = cb.and(
							Query.<T, Boolean>parse(cls, ((JsonString) serial.getParams().get(X)).getString(), cb, rt,
									c).getValue1(),
							Query.<T, Boolean>parse(cls, ((JsonString) serial.getParams().get(Y)).getString(), cb, rt,
									c).getValue1());
				else
					predicate = cb.and(serial.getRestrictions().stream().map(p -> parse(cls, p, cb, rt, c))
							.toArray(Predicate[]::new));
				break;
			case OR:
				if (serial.getParams().containsKey(X))
					predicate = cb.or(
							Query.<T, Boolean>parse(cls, ((JsonString) serial.getParams().get(X)).getString(), cb, rt,
									c).getValue1(),
							Query.<T, Boolean>parse(cls, ((JsonString) serial.getParams().get(Y)).getString(), cb, rt,
									c).getValue1());
				else
					predicate = cb.or(serial.getRestrictions().stream().map(p -> parse(cls, p, cb, rt, c))
							.toArray(Predicate[]::new));
				break;
			default:
				throw new RuntimeException();
		}
		return predicate;
	}

	public static <T extends Entity.Persistence<? extends Serializable>> PredicateBuilder<T> where(Class<T> cls,
			Parameters parameters) {
		return where(cls, parameters, (cb, rt, c, p) -> p);
	}

	public static <T extends Entity.Persistence<? extends Serializable>> OrderBuilder<T> order(Parameters parameters,
			OrderBuilder<T> ob) {
		return (cb, rt, o) -> {
			return ob.apply(cb, rt, o);
		};
	}

	public static <T extends Entity.Persistence<? extends Serializable>> OrderBuilder<T> order(Parameters parameters) {
		return order(parameters, (cb, rt, o) -> o);
	}

	public static <T> QueryBuilder.Bottleneck<T> paginate(int pageNumber, int pageSize) {
		return tq -> tq.setFirstResult((pageNumber - 1) * pageSize).setMaxResults(pageSize);
	}

	public static class Parameters {
		private Set<Predicate> where;

		public Set<Predicate> getWhere() {
			if (where == null)
				where = new HashSet<>();
			return where;
		}

		public void setWhere(Set<Predicate> where) {
			this.where = where;
		}

		public static class Predicate {
			private Type type;
			private Map<String, JsonValue> params;
			private List<Predicate> restrictions;

			public Predicate() {
				super();
			}

			public Type getType() {
				return type;
			}

			public void setType(Type type) {
				this.type = type;
			}

			public Map<String, JsonValue> getParams() {
				if (params == null)
					params = new HashMap<>();
				return params;
			}

			public void setParams(Map<String, JsonValue> params) {
				this.params = params;
			}

			public List<Predicate> getRestrictions() {
				if (restrictions == null)
					restrictions = new ArrayList<>();
				;
				return restrictions;
			}

			public void setRestrictions(List<Predicate> restrictions) {
				this.restrictions = restrictions;
			}

			public static enum Type {
				EQUAL, LIKE, AND, OR;
			}
		}

		public static class Pagination extends Parameters {
			private Integer pageNumber;
			private Integer pageSize;
			private List<Sorting> orderBy;

			public int getPageNumber() {
				if (pageNumber == null || pageNumber == 0)
					pageNumber = Integer.valueOf(1);
				return pageNumber;
			}

			public void setPageNumber(int pageNumber) {
				this.pageNumber = pageNumber;
			}

			public Integer getPageSize() {
				if (pageSize == null || pageSize == 0)
					pageSize = Integer.valueOf(5);
				return pageSize;
			}

			public void setPageSize(Integer pageSize) {
				this.pageSize = pageSize;
			}

			public List<Sorting> getOrderBy() {
				if (orderBy == null)
					orderBy = new ArrayList<>();
				return orderBy;
			}

			public void setOrderBy(List<Sorting> orderBy) {
				this.orderBy = orderBy;
			}

			public static class Sorting {
				private String column;
				private boolean desc;

				public Sorting() {
					super();
				}

				public String getColumn() {
					return column;
				}

				public void setColumn(String column) {
					this.column = column;
				}

				public boolean isDesc() {
					return desc;
				}

				public void setDesc(boolean desc) {
					this.desc = desc;
				}
			}
		}
	}

	public static interface Request<T> {
		T getParam();

		public static class Single<T> extends Parameters implements Request<T> {
			private T param;

			@Override
			public T getParam() {
				return param;
			}

			public void setParam(T param) {
				this.param = param;
			}
		}

		public static class Many<T> extends Parameters.Pagination implements Request<T> {
			private T param;

			@Override
			public T getParam() {
				return param;
			}

			public void setParam(T param) {
				this.param = param;
			}
		}
	}

	public static class Response<T> {
		private List<T> data;
		private Meta meta;
		private Links links;

		public List<T> getData() {
			return data;
		}

		public void setData(List<T> data) {
			this.data = data;
		}

		public Meta getMeta() {
			return meta;
		}

		public void setMeta(Meta meta) {
			this.meta = meta;
		}

		public Links getLinks() {
			return links;
		}

		public void setLinks(Links links) {
			this.links = links;
		}

		public static class Meta {
			private Integer pageNumber, pageSize, lastPageNumber;
			private Long totalCount;

			public Integer getPageNumber() {
				return pageNumber;
			}

			public void setPageNumber(Integer pageNumber) {
				this.pageNumber = pageNumber;
			}

			public Integer getPageSize() {
				return pageSize;
			}

			public void setPageSize(Integer pageSize) {
				this.pageSize = pageSize;
			}

			public Integer getLastPageNumber() {
				return lastPageNumber;
			}

			public void setLastPageNumber(Integer lastPageNumber) {
				this.lastPageNumber = lastPageNumber;
			}

			public Long getTotalCount() {
				return totalCount;
			}

			public void setTotalCount(Long totalCount) {
				this.totalCount = totalCount;
			}
		}

		public static class Links {
			private String self, first, prev, next, last;

			public String getSelf() {
				return self;
			}

			public void setSelf(String self) {
				this.self = self;
			}

			public String getFirst() {
				return first;
			}

			public void setFirst(String first) {
				this.first = first;
			}

			public String getPrev() {
				return prev;
			}

			public void setPrev(String prev) {
				this.prev = prev;
			}

			public String getNext() {
				return next;
			}

			public void setNext(String next) {
				this.next = next;
			}

			public String getLast() {
				return last;
			}

			public void setLast(String last) {
				this.last = last;
			}
		}
	}
}
