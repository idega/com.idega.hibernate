//$Id: ImmutableType.java 5744 2005-02-16 12:50:19Z oneovthafew $
package org.hibernate.type;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Superclass of nullable immutable types.
 * @author Gavin King
 */
public abstract class ImmutableType extends NullableType {

	private static final long serialVersionUID = 4573883787635288032L;

	public final Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) {
		return value;
	}

	@Override
	public final boolean isMutable() {
		return false;
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		return original;
	}

}