package com.idega.hibernate.demo;

/**
 * <p>
 * Car interface
 * </p>
 *  Last modified: $Date: 2006/09/19 23:58:12 $ by $Author: tryggvil $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.1 $
 */
public interface Car {

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#getId()
	 */
	public abstract Long getId();

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#getManufacturer()
	 */
	public abstract String getManufacturer();

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#setManufacturer(java.lang.String)
	 */
	public abstract void setManufacturer(String manufacturer);

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#getModel()
	 */
	public abstract String getModel();

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#setModel(java.lang.String)
	 */
	public abstract void setModel(String model);

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#getYear()
	 */
	public abstract int getYear();

	/* (non-Javadoc)
	 * @see com.idega.hibernate.demo.Car#setYear(int)
	 */
	public abstract void setYear(int year);
}