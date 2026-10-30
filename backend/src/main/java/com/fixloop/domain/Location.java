package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "location", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"building", "floor", "room"})
})
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String building;
    private String floor;
    private String room;

    @Column(name = "campus_zone")
    private String campusZone;
}
