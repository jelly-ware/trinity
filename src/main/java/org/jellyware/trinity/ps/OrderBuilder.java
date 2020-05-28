/**
 * 
 */
package org.jellyware.trinity.ps;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.jellyware.trinity.Entity;

/**
 * @author Jotter
 *
 */
@FunctionalInterface
public interface OrderBuilder<T extends Entity.Persistence<? extends Serializable>> {
	Set<Order> apply(CriteriaBuilder criteriaBuilder, Root<T> root, Set<Order> orders);
}
