package com.project.dogfaw.post.repository;

import com.project.dogfaw.post.model.Post;
import com.project.dogfaw.post.model.PostStack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostStackRepository extends JpaRepository<PostStack, Long> {

    List<PostStack> findByPostId(Long postId);
    List<PostStack> findByPost(Post post);




    @Query(value = "select stack from PostStack where id=:postId",nativeQuery = true)
    List<PostStack> findAll(@Param("postId") Long postId);

    List<PostStack> deleteByPostId(Long postId);

}
