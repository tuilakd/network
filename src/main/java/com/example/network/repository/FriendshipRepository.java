package com.example.network.repository;

import com.example.network.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :u1 AND f.addressee = :u2) OR (f.requester = :u2 AND f.addressee = :u1)")
    Optional<Friendship> findBetweenUsers(@Param("u1") String u1, @Param("u2") String u2);

    List<Friendship> findByAddresseeAndStatus(String addressee, String status);

    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' AND (f.requester = :username OR f.addressee = :username)")
    List<Friendship> findAllAcceptedFriends(@Param("username") String username);
}
