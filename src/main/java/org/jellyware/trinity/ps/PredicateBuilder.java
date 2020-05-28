/**
 * 
 */
package org.jellyware.trinity.ps;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.jellyware.trinity.Entity;

/**
 * @author Jotter
 *
 */
@FunctionalInterface
public interface PredicateBuilder<T extends Entity.Persistence<? extends Serializable>> {
	Set<Predicate> apply(CriteriaBuilder criteriaBuilder, Root<T> root, CommonAbstractCriteria commonAbstractCriteria,
			Set<Predicate> predicates);
}
