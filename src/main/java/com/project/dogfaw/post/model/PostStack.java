package com.project.dogfaw.post.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
@DynamicUpdate // null 값인 field 를 DB에서 설정된 default을 줌
public class PostStack {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column
    private String stack;

//    @JsonIgnore
//    @ManyToOne
//    @JoinColumn(name = "P_ID")
//    private Post postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_p_id")
    private Post post;

//    public void setPost(Post post) {
//        this.post = post;
//    }


    public PostStack(Post post, String stack) {
        this.post = post;
        this.stack = stack;
    }

    public void updatePostStack(String stack, Post post) {
        this.stack = stack;
        this.post = post;
    }


}
