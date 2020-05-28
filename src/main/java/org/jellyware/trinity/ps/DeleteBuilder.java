/**
 * 
 */
package org.jellyware.trinity.ps;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import org.jellyware.trinity.Entity;

/**
 * @author Jotter
 *
 */
@FunctionalInterface
public interface DeleteBuilder<T extends Entity.Persistence<? extends Serializable>> {
	CriteriaDelete<T> apply(CriteriaBuilder criteriaBuilder, Root<T> root, CriteriaDelete<T> criteriaDelete);
}
