/**
 * 
 */
package org.jellyware.trinity.ps;

import java.io.Serializable;
import java.util.function.UnaryOperator;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.jellyware.trinity.Entity;

/**
 * @author Jotter
 *
 */
@FunctionalInterface
public interface QueryBuilder<T extends Entity.Persistence<? extends Serializable>, U> {

	CriteriaQuery<U> apply(CriteriaBuilder criteriaBuilder, Root<T> root, CriteriaQuery<U> criteriaQuery);

	public static interface Bottleneck<T> extends UnaryOperator<TypedQuery<T>> {
		/**
		 * Returns a bottleneck that always returns its input query.
		 *
		 * @param <T> the type of the input and output of the operator
		 * @return a unary operator that always returns its input argument
		 */
		static <T> Bottleneck<T> free() {
			return t -> t;
		}

		public static enum GraphType {
			FETCH("javax.persistence.fetchgraph"), LOAD("javax.persistence.loadgraph");

			private String key;

			GraphType(String key) {
				this.key = key;
			}

			public String getKey() {
				return key;
			}
		}
	}
}
