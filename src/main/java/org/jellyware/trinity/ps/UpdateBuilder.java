/**
 * 
 */
package org.jellyware.trinity.ps;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import org.jellyware.trinity.Entity;

/**
 * @author Jotter
 *
 */
@FunctionalInterface
public interface UpdateBuilder<T extends Entity.Persistence<? extends Serializable>> {
	CriteriaUpdate<T> apply(CriteriaBuilder criteriaBuilder, Root<T> root, CriteriaUpdate<T> criteriaUpdate);
}
