package com.fixloop.repository;

import com.fixloop.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Integer> {

    Optional<Location> findByBuildingAndFloorAndRoom(String building, String floor, String room);

    @Query("SELECT DISTINCT l.building FROM Location l ORDER BY l.building")
    List<String> findDistinctBuildings();

    List<Location> findByBuilding(String building);

    List<Location> findByBuildingAndFloor(String building, String floor);
}
