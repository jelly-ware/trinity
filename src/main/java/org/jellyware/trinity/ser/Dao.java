/**
 * 
 */
package org.jellyware.trinity.ser;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

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
public class Dao<T extends Entity.Model<K>, S extends Entity.Persistence<K>, U extends Entity.Serial<K>, K extends Serializable>
		implements org.jellyware.trinity.Dao.Serial<T, S, U, K> {
	private final org.jellyware.trinity.Dao.Model<T, S, K> mdl;
	private final BiConsumer<U, T> deserialize;
	private final BiConsumer<T, U> serialize;
	private final Class<U> cls;

	@SuppressWarnings("unchecked")
	public Dao(org.jellyware.trinity.Dao.Model<T, S, K> mdl, BiConsumer<T, U> serialize, BiConsumer<U, T> deserialize) {
		super();
		Objects.requireNonNull(mdl, "Model Dao cannot be null");
		Objects.requireNonNull(serialize, "Serialize cannot be null");
		Objects.requireNonNull(deserialize, "Deserialize cannot be null");
		this.mdl = mdl;
		this.serialize = serialize;
		this.deserialize = deserialize;
		// resolve entity class
		ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
		this.cls = (Class<U>) parameterizedType.getActualTypeArguments()[2];
	}

	public Dao(org.jellyware.trinity.Dao.Model<T, S, K> mdl, Class<U> cls, BiConsumer<T, U> serialize,
			BiConsumer<U, T> deserialize) {
		super();
		Objects.requireNonNull(mdl, "Model Dao cannot be null");
		Objects.requireNonNull(cls, "Class cannot be null");
		Objects.requireNonNull(serialize, "Serialize cannot be null");
		Objects.requireNonNull(deserialize, "Deserialize cannot be null");
		this.mdl = mdl;
		this.serialize = serialize;
		this.deserialize = deserialize;
		this.cls = cls;
	}

	@Override
	public Model<T, S, K> mdl() {
		return mdl;
	}

	@Override
	public Class<T> cls() {
		return mdl().cls();
	}

	@Override
	public Class<U> ser() {
		return cls;
	}

	@Override
	public T persist(T entity) {
		return mdl.persist(entity);
	}

	@Override
	public Optional<T> find(QueryBuilder<S, Tuple> qb, Bottleneck<Tuple> bottleneck) {
		return mdl.find(qb, bottleneck);
	}

	@Override
	public <V> Optional<V> find(Class<V> projection, QueryBuilder<S, V> qb, Bottleneck<V> bottleneck) {
		return mdl.find(projection, qb, bottleneck);
	}

	@Override
	public Stream<T> select(QueryBuilder<S, Tuple> qb, Bottleneck<Tuple> bottleneck) {
		return mdl.select(qb, bottleneck);
	}

	@Override
	public <V> Stream<V> select(Class<V> projection, QueryBuilder<S, V> qb, Bottleneck<V> bottleneck) {
		return mdl.select(projection, qb, bottleneck);
	}

	@Override
	public void delete(DeleteBuilder<S> db) {
		mdl.delete(db);
	}

	@Override
	public void update(UpdateBuilder<S> ub) {
		mdl.update(ub);
	}

	@Override
	public final void deserialize(U serialObject, T entity) {
		Objects.requireNonNull(serialObject, "Serial Object cannot be null");
		Objects.requireNonNull(entity, "Entity cannot be null");
		deserialize.accept(serialObject, entity);
	}

	@Override
	public final void serialize(T entity, U serialObject) {
		Objects.requireNonNull(serialObject, "Serial Object cannot be null");
		Objects.requireNonNull(entity, "Entity cannot be null");
		serialize.accept(entity, serialObject);
	}
}
