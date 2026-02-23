package com.rdapp.deploy.repository;

import com.rdapp.deploy.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, String> {

    @Query("SELECT DISTINCT a FROM Application a " +
           "LEFT JOIN FETCH a.deployments " +
           "ORDER BY a.sortOrder ASC, a.name ASC")
    List<Application> findAllWithDeployments();

    @Query("SELECT DISTINCT a FROM Application a " +
           "LEFT JOIN FETCH a.deployments " +
           "LEFT JOIN FETCH a.commits " +
           "WHERE a.id = :id")
    Application findByIdWithAll(String id);
}
