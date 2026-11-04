package com.example.network.repository;

import com.example.network.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    boolean existsByRoomIdAndUsername(Long roomId, String username);
    List<RoomMember> findByUsername(String username);
    List<RoomMember> findByRoomId(Long roomId);
    void deleteByUsername(String username);
}