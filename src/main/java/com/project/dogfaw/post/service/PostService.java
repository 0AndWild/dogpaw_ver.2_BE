package com.project.dogfaw.post.service;


import com.project.dogfaw.acceptance.repository.AcceptanceRepository;
import com.project.dogfaw.apply.model.UserApplication;
import com.project.dogfaw.apply.repository.UserApplicationRepository;
import com.project.dogfaw.bookmark.model.BookMark;
import com.project.dogfaw.bookmark.repository.BookMarkRepository;
import com.project.dogfaw.comment.repository.CommentRepository;
import com.project.dogfaw.common.exception.CustomException;
import com.project.dogfaw.common.exception.ErrorCode;
import com.project.dogfaw.common.exception.StatusResponseDto;
import com.project.dogfaw.post.dto.MyApplyingResponseDto;
import com.project.dogfaw.post.dto.PostDetailResponseDto;
import com.project.dogfaw.post.dto.PostRequestDto;
import com.project.dogfaw.post.model.Post;
import com.project.dogfaw.post.model.PostStack;
import com.project.dogfaw.post.model.UserStatus;
import com.project.dogfaw.post.repository.PostRepository;
import com.project.dogfaw.post.repository.PostStackRepository;
import com.project.dogfaw.user.model.User;
import com.project.dogfaw.user.model.UserRoleEnum;
import com.project.dogfaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostStackRepository postStackRepository;
    private final BookMarkRepository bookMarkRepository;
    private final UserApplicationRepository userApplicationRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AcceptanceRepository acceptanceRepository;



    //????????????
    public Map<String, Object> allPost(User user, long page) {
        try {
            PageRequest pageRequest = PageRequest.of((int) page, 24);

            //?????? ????????? ??????????????????
            Slice<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageRequest);

            //BookMarkStatus??? ??????????????? ????????? ArrayList ??????
            ArrayList<MyApplyingResponseDto> postList = new ArrayList<>();
            //true || false ?????? ????????? Boolean type??? bookMarkStatus ????????? ?????? ??????
            Boolean bookMarkStatus = false;


            HashSet<Long> bookmarkPostId = new HashSet<>();

            //???????????? ????????? ???????????? ?????????????????? ??????????????? bookMarkStatus??? ?????? false??? ??????
            if (user == null) {
                for (Post post : posts) {
                    User writer = post.getUser();
                    //Stacks(????????????) ????????????
                    List<PostStack> stringPostStacks = post.getPostStacks();

                    //PostResponseDto??? ????????? ????????????, ????????? ??????,writer ??? ?????? ????????? ????????? ????????? ???????????? ???????????? ??????
                    MyApplyingResponseDto mainDTO = new MyApplyingResponseDto(post, stringPostStacks, bookMarkStatus, writer);
                    //?????? ????????? ArrayList??? ????????? ????????? ?????? ?????????
                    postList.add(mainDTO);
                }
            } else {
                List<BookMark> bookMarkLists = bookMarkRepository.findAllByUser(user);
                for (BookMark BmPostId : bookMarkLists){
                    bookmarkPostId.add(BmPostId.getPost().getId());
                }
                
                for (Post post: posts){
                    User writer = post.getUser();
                    bookMarkStatus = bookmarkPostId.contains(post.getId());
                    List<PostStack> stringPostStacks = post.getPostStacks();
                    //PostResponseDto??? ????????? ????????????, ????????? ??????,writer ??? ?????? ????????? ????????? ????????? ???????????? ???????????? ??????
                    MyApplyingResponseDto mainDTO = new MyApplyingResponseDto(post, stringPostStacks, bookMarkStatus, writer);
                    //?????? ????????? ArrayList??? ????????? ????????? ?????? ?????????
                    postList.add(mainDTO);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("postList", postList);
            data.put("isLast", posts.isLast());
            return data;
        }catch (Exception e){
         throw new CustomException(ErrorCode.POST_ALL_LOAD_FAIL);
        }
    }

    // post ??????
    @Transactional
    public MyApplyingResponseDto postPost(PostRequestDto postRequestDto, User user) {
        // ????????? ????????? ?????? (?????? ????????? -> user?????? post??? ?????? post add)
        Post post = postRepository.save(new Post(postRequestDto, user));
//        PostResponseDto postResponseDto = new PostResponseDto(post,false, user);
        // ????????? Post -> PostResponseDto??? ?????? ??????
//        return postResponseDto;

        List<String> stacks = postRequestDto.getStacks();
        List<PostStack> stringPostStacks = new ArrayList<>();
        for(String stack : stacks){
            PostStack postStack = new PostStack(post, stack);
            stringPostStacks.add(postStack);
            postStackRepository.save(postStack);

        }

        return new MyApplyingResponseDto(post, stringPostStacks, false, user);

    }


    //post ????????????


    public PostDetailResponseDto getPostDetail(Long postId, User user) {
        //????????? ????????????
        Post post = postRepository.findById(postId).orElseThrow(
                ()-> new CustomException(ErrorCode.POST_NOT_FOUND)
        );

        //????????? ???
        List<UserApplication> applicationList =post.getUserApplications();
        int applierCnt = applicationList.size();

        /*???????????????????????? ???????????? ????????????*/
        if (user == null) {
            String userStatus = UserStatus.USER_STATUS_ANONYMOUS.getUserStatus();
            //??????????????? ?????? ????????? bookmarkStatus ?????? false
            //???????????????
            Boolean bookMarkStatus = false;
            List<PostStack> postStacks = postStackRepository.findByPostId(postId);
            List<String> stringPostStacks = new ArrayList<>();
            for(PostStack postStack : postStacks){
                stringPostStacks.add(postStack.getStack());
            }
            return new PostDetailResponseDto(post, stringPostStacks, user, bookMarkStatus,applierCnt,userStatus);
        }else {

        /*
        1.???????????? ??? ????????? ???????????? ????????? ?????? ?????? : userStatus = author
        2.???????????? ????????? ????????? ????????? ?????? : userStatus = participant
        3.???????????? ????????? ????????? ????????? ?????? : userStatus = applicant
        4.???????????? ????????? ???????????? ?????? ????????? ?????? : userStatus = member
        */
            //userStatus ??????
            String checkName = user.getUsername();
            String nickname = post.getUser().getUsername(); // ?????? ????????? ????????? ?????????

            //????????????
            Boolean applyingStatus = userApplicationRepository.existsByUserAndPost(user, post);
            //??????????????????
            Boolean acceptedStatus = acceptanceRepository.existsByUserAndPost(user, post);

            String userStatus;
            if (user.getRole()== UserRoleEnum.ADMIN){
                userStatus = UserStatus.USER_STATUS_MASTER.getUserStatus();
            } else if (checkName.equals(nickname)) {
                userStatus = UserStatus.USER_STATUS_AUTHOR.getUserStatus();
            } else if (Boolean.TRUE.equals(acceptedStatus)) {
                userStatus = UserStatus.USER_STATUS_PARTICIPANT.getUserStatus();
            } else if (Boolean.TRUE.equals(applyingStatus)) {
                userStatus = UserStatus.USER_STATUS_APPLICANT.getUserStatus();
            } else {
                userStatus = UserStatus.USER_STATUS_MEMBER.getUserStatus();
            }

            //???????????????
            Boolean bookMarkStatus = bookMarkRepository.existsByUserAndPost(user, post);

            List<PostStack> postStacks = postStackRepository.findByPostId(postId);
            List<String> stringPostStacks = new ArrayList<>();
            for (PostStack postStack : postStacks) {
                stringPostStacks.add(postStack.getStack());
            }

            return new PostDetailResponseDto(post, stringPostStacks, user, bookMarkStatus, applierCnt, userStatus);
        }
    }

    //????????? ??????
    @Transactional
    public void updatePost(Long postId, PostRequestDto postRequestDto, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new CustomException(ErrorCode.POST_NOT_FOUND)
        );
        User user = userRepository.findByUsername(username)
                .orElseThrow(()->new CustomException(ErrorCode.NOT_MATCH_USER_INFO)
        );
        if (!Objects.equals(username, post.getUser().getUsername())){
            throw new CustomException(ErrorCode.POST_UPDATE_WRONG_ACCESS);
        }
        postStackRepository.deleteByPostId(postId);
        for (String stack : postRequestDto.getStacks()){
            postStackRepository.save(new PostStack(post,stack));
        }

        post.update(postRequestDto, user.getId());
    }

    //????????? ??????
    @Transactional
    public void deletePost(Long postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new CustomException(ErrorCode.POST_NOT_FOUND)
        );
        if(user.getUsername().equals(post.getUser().getUsername()) || user.getRole()== UserRoleEnum.ADMIN ){
            userApplicationRepository.deleteAllByPost(post);
            acceptanceRepository.deleteAllByPost(post);
            bookMarkRepository.deleteAllByPost(post);
            commentRepository.deleteAllByPost(post);
            postRepository.deleteById(postId);
        } else {
            throw new CustomException(ErrorCode.POST_INQUIRY_NO_AUTHORITY);
        }

    }

//    private List<PostStack> tostackByPostId(List<StackDto> requestDto, Post post) {
//        List<PostStack> stackList = new ArrayList<>();
//        for(StackDto stackdto : requestDto){
//            stackList.add(new PostStack(stackdto, post ));
//        }
//        return stackList;
//    }



    //????????? ??????
    public ArrayList<MyApplyingResponseDto> bookMarkRank(User user) {
        try {
            PageRequest pageRequest = PageRequest.of(0, 3);

            List<Post> posts = postRepository.findByOrderByBookmarkCntDesc(pageRequest);
            ArrayList<MyApplyingResponseDto> postList = new ArrayList<>();
            HashSet<Long> bookmarkPostId = new HashSet<>();
            Boolean bookMarkStatus = false;

            if (user == null) {
                for (Post post : posts) {
                    Long postId = post.getId();
                    User writer = post.getUser();
                    //???????????????(????????????) ????????? ??????
                    List<PostStack> stringPostStacks = postStackRepository.findByPostId(postId);
                    MyApplyingResponseDto postDto = new MyApplyingResponseDto(post, stringPostStacks, bookMarkStatus, writer);
                    postList.add(postDto);
                }
            } else {
                List<BookMark> bookMarkLists = bookMarkRepository.findAllByUser(user);
                for (BookMark BmPostId : bookMarkLists){
                    bookmarkPostId.add(BmPostId.getPost().getId());
                }

                for (int i = 0; i<posts.size(); i++){
                    Post post = posts.get(i);
                    User writer = posts.get(i).getUser();
                    bookMarkStatus = bookmarkPostId.contains(post.getId());
                    List<PostStack> stringPostStacks = post.getPostStacks();

                    MyApplyingResponseDto postDto = new MyApplyingResponseDto(post, stringPostStacks, bookMarkStatus, writer);
                    postList.add(postDto);
                }
            }
            return postList;
        }catch (Exception e){
            throw new CustomException(ErrorCode.POST_RANK_LOAD_FAIL);
        }
    }
    @Transactional
    /*????????????,???????????? ??????(????????????)*/
    public ResponseEntity<Object> updateDeadline(Long postId, User user) {
        //?????????
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        //????????? ????????? ??????
        if (!user.getNickname().equals(post.getUser().getNickname())) {
            throw new CustomException(ErrorCode.POST_INQUIRY_NO_AUTHORITY);
        }
        if (post.getDeadline() == false) {
            Boolean deadline = true;
            post.updateDeadline(deadline);
            return new ResponseEntity(new StatusResponseDto("????????? ?????????????????????",true), HttpStatus.OK);
        } else {
            //?????? ?????????????????? ?????? ?????? ?????? ???????????? ??????
            if (post.getCurrentMember() >= post.getMaxCapacity()) {
                throw new CustomException(ErrorCode.POST_PEOPLE_SET_CLOSED);
            }
            Boolean deadline = false;
            post.updateDeadline(deadline);
            return new ResponseEntity(new StatusResponseDto("?????? ????????? ?????????????????????",false), HttpStatus.OK);
        }
    }
}



