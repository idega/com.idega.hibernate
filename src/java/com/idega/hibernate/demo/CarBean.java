package com.idega.hibernate.demo;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="CAR")
public class CarBean implements Serializable, Car
{
	@Id
    private Long car_id;
    private String manufacturer;
    private String model;
    private int year;

    public CarBean() {}


    public Long getId() {
        return car_id;
    }
    
    @SuppressWarnings("unused")
		private void setId(Long car_id) {	   //Note private visibility
        this.car_id = car_id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}