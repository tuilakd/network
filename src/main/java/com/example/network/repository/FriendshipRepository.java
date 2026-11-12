package com.example.network.repository;

import com.example.network.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByAddresseeAndStatus(String addressee, String status);

    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' AND (f.requester = :username OR f.addressee = :username)")
    List<Friendship> findAllAcceptedFriends(@Param("username") String username);

    @Query("SELECT f FROM Friendship f WHERE (f.requester = :u1 AND f.addressee = :u2) OR (f.requester = :u2 AND f.addressee = :u1)")
    List<Friendship> findAllBetweenUsers(@Param("u1") String u1, @Param("u2") String u2);

    default Optional<Friendship> findBetweenUsers(String u1, String u2) {
        return findAllBetweenUsers(u1, u2).stream().findFirst();
    }

    void deleteByRequesterOrAddressee(String requester, String addressee);
}