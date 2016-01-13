//$Id: NullableType.java 9659 2006-03-20 14:01:19Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.compare.EqualsHelper;

/**
 * Superclass of single-column nullable types.
 * @author Gavin King
 */
public abstract class NullableType extends AbstractType {

	private static final long serialVersionUID = 7655708492981840569L;

	private static final boolean IS_TRACE_ENABLED;
	static {
		//cache this, because it was a significant performance cost; is
		// trace logging enabled on the type package...
		IS_TRACE_ENABLED = LogFactory.getLog( StringHelper.qualifier( Type.class.getName() ) ).isTraceEnabled();
	}

	/**
	 * Get a column value from a result set, without worrying about the
	 * possibility of null values
	 */
	public abstract Object get(ResultSet rs, String name)
	throws HibernateException, SQLException;

	/**
	 * Get a parameter value without worrying about the possibility of null values
	 */
	public abstract void set(PreparedStatement st, Object value, int index)
	throws HibernateException, SQLException;

	public abstract int sqlType();

	public abstract String toString(Object value) throws HibernateException;

	public abstract Object fromStringValue(String xml) throws HibernateException;

	@Override
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session)
	throws HibernateException, SQLException {
		if ( settable[0] ) nullSafeSet(st, value, index);
	}

	@Override
	public final void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
	throws HibernateException, SQLException {
		nullSafeSet(st, value, index);
	}

	public final void nullSafeSet(PreparedStatement st, Object value, int index)
	throws HibernateException, SQLException {
		try {
			if ( value == null ) {
				if ( IS_TRACE_ENABLED ) {
					log().trace( "binding null to parameter: " + index );
				}

				st.setNull( index, sqlType() );
			}
			else {
				if ( IS_TRACE_ENABLED ) {
					log().trace( "binding '" + toString( value ) + "' to parameter: " + index );
				}

				set( st, value, index );
			}
		}
		catch ( RuntimeException re ) {
			log().info( "could not bind value '" + toString( value ) + "' to parameter: " + index + "; " + re.getMessage() );
			throw re;
		}
		catch ( SQLException se ) {
			log().info( "could not bind value '" + toString( value ) + "' to parameter: " + index + "; " + se.getMessage() );
			throw se;
		}
	}

	@Override
	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner)
	throws HibernateException, SQLException {
		return nullSafeGet(rs, names[0]);
	}

	public final Object nullSafeGet(ResultSet rs, String[] names)
	throws HibernateException, SQLException {
		return nullSafeGet(rs, names[0]);
	}

	public final Object nullSafeGet(ResultSet rs, String name)
	throws HibernateException, SQLException {
		try {
			Object value = get(rs, name);
			if ( value == null || rs.wasNull() ) {
				if ( IS_TRACE_ENABLED ) {
					log().trace( "returning null as column: " + name );
				}
				return null;
			}
			else {
				if (IS_TRACE_ENABLED) {
					log().trace( "returning '" + toString( value ) + "' as column: " + name );
				}
				return value;
			}
		}
		catch ( RuntimeException re ) {
			log().info( "could not read column value from result set: " + name + "; " + re.getMessage() );
			throw re;
		}
		catch ( SQLException se ) {
			log().info( "could not read column value from result set: " + name + "; " + se.getMessage() );
			throw se;
		}
	}

	@Override
	public final Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
	throws HibernateException, SQLException {
		return nullSafeGet(rs, name);
	}

	public final String toXMLString(Object value, SessionFactoryImplementor pc)
	throws HibernateException {
		return toString(value);
	}

	public final Object fromXMLString(String xml, Mapping factory) throws HibernateException {
		return xml==null || xml.length()==0 ? null : fromStringValue(xml);
	}

	@Override
	public final int getColumnSpan(Mapping session) {
		return 1;
	}

	@Override
	public final int[] sqlTypes(Mapping session) {
		return new int[] { sqlType() };
	}

	public final boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return isEqual(x, y);
	}

	@Override
	public boolean isEqual(Object x, Object y) {
		return EqualsHelper.equals(x, y);
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return value==null ? "null" : toString(value);
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return fromXMLString( xml.getText(), factory );
	}

	public void setToXMLNode(Node xml, Object value, SessionFactoryImplementor factory)
	throws HibernateException {
		xml.setText( toXMLString(value, factory) );
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value==null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session)
	throws HibernateException {
		return checkable[0] && isDirty(old, current, session);
	}

	private Log log() {
		return LogFactory.getLog( getClass() );
	}
}