package com.idega.hibernate.demo;


import java.io.Serializable;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="DRIVER")
public class Driver implements Serializable {

	private static final long serialVersionUID = 6137563762021348932L;

	@Id
    private Long driver_id;

    private String name;
    private int age;
    
    @OneToMany(cascade=CascadeType.ALL)
    private Set<CarBean> carsOwned;

    public Driver() {
    	//Empty constructor
    }

    public Long getId() {
        return this.driver_id;
    }
    
	@SuppressWarnings("unused")
	private void setId(Long driver_id) {	  //Note private visibility
        this.driver_id = driver_id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return this.age;
    }
    public void setAge(int age) {
        this.age = age;
    }    
    public Set<CarBean> getCarsOwned() {
        return this.carsOwned;
    }
    public void setCarsOwned(Set<CarBean> carsOwned) {
        this.carsOwned = carsOwned;
    }
}