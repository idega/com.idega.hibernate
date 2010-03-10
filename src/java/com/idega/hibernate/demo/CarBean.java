package com.idega.hibernate.demo;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="CAR")
public class CarBean implements Serializable, Car
{
	private static final long serialVersionUID = -464315998566500332L;

	@Id
    private Long car_id;
    private String manufacturer;
    private String model;
    private int year;

    public CarBean() {
    	//Empty constructor
    }


    public Long getId() {
        return this.car_id;
    }
    
	@SuppressWarnings("unused")
	private void setId(Long car_id) {	   //Note private visibility
        this.car_id = car_id;
    }

    public String getManufacturer() {
        return this.manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return this.year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}