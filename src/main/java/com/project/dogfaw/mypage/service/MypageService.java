package com.project.dogfaw.mypage.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.project.dogfaw.acceptance.model.Acceptance;
import com.project.dogfaw.acceptance.repository.AcceptanceRepository;
import com.project.dogfaw.apply.model.UserApplication;
import com.project.dogfaw.apply.repository.UserApplicationRepository;
import com.project.dogfaw.bookmark.model.BookMark;
import com.project.dogfaw.bookmark.repository.BookMarkRepository;
import com.project.dogfaw.common.exception.CustomException;
import com.project.dogfaw.common.exception.ErrorCode;
import com.project.dogfaw.common.exception.StatusResponseDto;
import com.project.dogfaw.mypage.dto.*;
import com.project.dogfaw.post.dto.MyApplyingResponseDto;
import com.project.dogfaw.post.model.Post;
import com.project.dogfaw.post.model.PostStack;
import com.project.dogfaw.post.repository.PostRepository;
import com.project.dogfaw.post.repository.PostStackRepository;
import com.project.dogfaw.sse.model.NotificationType;
import com.project.dogfaw.sse.service.NotificationService;
import com.project.dogfaw.user.dto.StackDto;
import com.project.dogfaw.user.dto.UserInfo;
import com.project.dogfaw.user.model.Stack;
import com.project.dogfaw.user.model.User;
import com.project.dogfaw.user.repository.StackRepository;
import com.project.dogfaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MypageService {

    private final PostRepository postRepository;
    private final BookMarkRepository bookMarkRepository;
    private final PostStackRepository postStackRepository;
    private final UserApplicationRepository userApplicationRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final StackRepository stackRepository;
    private final UserRepository userRepository;

    private final AmazonS3Client amazonS3Client;

    private final NotificationService notificationService;

    private final EntityManager em;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /*내가 북마크한 글 조회*/
    public ArrayList<MyBookmarkResponseDto> myBookmark(User user) {

        List<BookMark> userPosts = bookMarkRepository.findAllByUserOrderByIdDesc(user);
        ArrayList<MyBookmarkResponseDto> postList = new ArrayList<>();
        //로그인한 유저가 북마크한 글들을 불러와 해당 게시글의 정보를 MyBookmarkResponseDto에 담아 리턴
        for (BookMark userPost : userPosts) {
            Post userPosting = userPost.getPost();
            User writer = userPosting.getUser();
            List<String> stringPostStacks = new ArrayList<>();
            userPosting.getPostStacks().forEach((postStack) -> stringPostStacks.add(postStack.getStack()));

            MyBookmarkResponseDto postDto = new MyBookmarkResponseDto(userPosting, stringPostStacks, writer);

            postList.add(postDto);
        }

        return postList;
    }

    /*내가 작성한 글 조회*/
    public ArrayList<MyPostResponseDto> myPost(User user) {

        List<Post> posts = postRepository.findAllByUserOrderByIdDesc(user);
        List<BookMark> bmList = bookMarkRepository.findAllByUser(user);

        ArrayList<MyPostResponseDto> postList = new ArrayList<>();
        HashSet<Long> bm_PostId = new HashSet<>();
        Boolean bookMarkStatus = false;

        //로그인한 유저가 북마크한 게시글 postId를 HashSet에 삽입
        bmList.forEach((bm) -> bm_PostId.add(bm.getPost().getId()));

        //북마크여부, 자신이 작성한 글의 기술 스택 담아 리턴
        for (Post post : posts) {
            Long postId = post.getId();
            User writer = post.getUser();
            //북마크여부
            bookMarkStatus = bm_PostId.contains(postId);
            //기술스택
            List<String> stringPostStacks = new ArrayList<>();
            post.getPostStacks().forEach((postStack) -> stringPostStacks.add(postStack.getStack()));
            MyPostResponseDto postDto = new MyPostResponseDto(post, stringPostStacks, bookMarkStatus, writer);

            postList.add(postDto);
        }
        return postList;
    }

    /*내가 지원한 프로젝트 조회*/
    public ArrayList<MyApplyingResponseDto> myApply(User user) {

        //유저가 참여신청한 것을 리스트로 모두 불러옴
        List<UserApplication> userApply = userApplicationRepository.findAllByUserOrderByIdDesc(user);
        //유저가 북마크한 것을 리스트로 모두 불러옴
        List<BookMark> userBookmarks = bookMarkRepository.findAllByUser(user);

        ArrayList<Post> userApplying = new ArrayList<>();
        HashSet<Long> bm_List = new HashSet<>();
        ArrayList<MyApplyingResponseDto> postList = new ArrayList<>();
        Boolean bookMarkStatus = false;

        //유저가 지원신청한 게시글들을 ArrayList에 담아줌
        userApply.forEach(ua->userApplying.add(ua.getPost()));
        //유저가 북마크한 게시글의 Long id를 HashSet에 담아줌
        userBookmarks.forEach(bm->bm_List.add(bm.getPost().getId()));

        //일치하면 bookMarkStatus = true 아니면 false를 bookMarkStatus에 담아줌
        for (Post post : userApplying) {
            User writer = post.getUser();
            //북마크 여부
            bookMarkStatus = bm_List.contains(post.getId());
            //기술스택
            List<PostStack> stringPostStacks = post.getPostStacks();
            MyApplyingResponseDto postDto = new MyApplyingResponseDto(post, stringPostStacks, bookMarkStatus, writer);
            //아까 생성한 ArrayList에 새로운 모양의 값을 담아줌
            postList.add(postDto);
        }
        return postList;
    }

    /*참여수락된프로젝트조회*/
    public ArrayList<MyAcceptanceResponseDto> participation(User user) {
        //해당 유저의 참여완료된(수락된) 리스트
        List<Acceptance> acceptances = acceptanceRepository.findAllByUserOrderByIdDesc(user);
        //해당 유저의 북마크 리스트
        List<BookMark> bookMarks = bookMarkRepository.findAllByUser(user);

        ArrayList<Post> acceptedList = new ArrayList<>();
        HashSet<Long> bookMarkedList = new HashSet<>();
        ArrayList<MyAcceptanceResponseDto> postList = new ArrayList<>();
        Boolean bookMarkStatus = false;

        //해당 유저의 참여완료된 게시글 ArrayList에 담아줌
        acceptances.forEach(ac->acceptedList.add(ac.getPost()));
        //해당 유저가 북마크한 글들 HashSet에 담아줌
        bookMarks.forEach(bm->bookMarkedList.add(bm.getPost().getId()));

        for (Post accepted : acceptedList) {
            Long acceptedId = accepted.getId();
            User writer = accepted.getUser();
            //북마크여부
            bookMarkStatus = bookMarkedList.contains(acceptedId);
            //기술스택
            List<PostStack> postStacks = accepted.getPostStacks();
            List<String> stringPostStacks = new ArrayList<>();
            postStacks.forEach(ps->stringPostStacks.add(ps.getStack()));

            MyAcceptanceResponseDto postDto = new MyAcceptanceResponseDto(accepted, stringPostStacks, bookMarkStatus, writer);
            postList.add(postDto);

        }
        return postList;
    }


    /*지원자 전체조회(작성자만)*/
    public ArrayList<AllApplicantsDto> allApplicants(Long postId, User user) {
        //모집글 존재여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Long writer = post.getUser().getId();
        //작성자 일치 확인
        if (!writer.equals(user.getId())) {
            throw new CustomException(ErrorCode.MYPAGE_INQUIRY_NO_AUTHORITY);
        }
        //해당 게시글의 참여신청을 정보를 다 가져오고 해당 유저 정보를 뽑아와 dto에 담아 리스트로 반환
        List<UserApplication> applicants = userApplicationRepository.findAllByPostOrderByIdDesc(post);

        ArrayList<AllApplicantsDto> users = new ArrayList<>();


        for (UserApplication applicant : applicants) {
            User applier = applicant.getUser();
            List<String> stackList = new ArrayList<>();
            List<Stack> stacks = applier.getStacks();
            //지원자의 기술스택
            stacks.forEach(stack->stackList.add(stack.getStack()));

            AllApplicantsDto allApplicantsDto = new AllApplicantsDto(applier, stackList);
            users.add(allApplicantsDto);
        }
        return users;
    }

    /*이미지업로드 없이 나머지 유저정보만 편집할때*/
    @Transactional
    public void updateProfile(MypageRequestDto requestDto, User user) {
        //*닉네임 중복검사 후 S3업로드 및 편집
        String nickname = requestDto.getNickname();
        //현재 사용하고 있는 닉네임은 사용가능
        if (!user.getNickname().equals(nickname)) {
            if (userRepository.existsByNickname(nickname)) {
                throw new CustomException(ErrorCode.SIGNUP_NICKNAME_DUPLICATE);
            }
        }
        Long userId = user.getId();
        stackRepository.deleteAllByUserId(userId);
        user.updateProfile(requestDto);

//        user.setNickname(requestDto.getNickname());
        List<Stack> stack = stackRepository.saveAll(tostackByUserId(requestDto.getStacks(), user));
        user.updateStack(stack);
    }

    @Transactional
    /*프로필 기본이미지로 변경 요청*/
    public void basicImg(User user) {
        //아마존 S3에 저장된 이미지 삭제
        if (user.getProfileImg() != null) {
            String imgKey = user.getImgkey();
            amazonS3Client.deleteObject(bucket, imgKey);
        }
        user.basicImg();
        userRepository.save(user);
    }

    /*List<String> 형태로 변환*/
    private List<Stack> tostackByUserId(List<StackDto> requestDto, User user) {
        List<Stack> stackList = new ArrayList<>();
        for (StackDto stackdto : requestDto) {
            stackList.add(new Stack(stackdto, user));
        }
        return stackList;
    }


    /*내 팀원보기*/
    public ArrayList<AllTeammateDto> checkTeammate(Long postId) {
        //모집글 존재여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        //해당 게시글 수락 리스트 가져오기
        List<Acceptance> teammates = acceptanceRepository.findAllByPost(post);

        ArrayList<AllTeammateDto> users = new ArrayList<>();

        for (Acceptance teammate : teammates) {
            User teammateUser = teammate.getUser();
            List<String> stackList = new ArrayList<>();
            List<Stack> stacks = teammateUser.getStacks();
            stacks.forEach(stack->stackList.add(stack.getStack()));
            AllTeammateDto allTeammateDto = new AllTeammateDto(teammateUser, stackList);
            users.add(allTeammateDto);
        }
        return users;
    }

    /*팀원 추방하기*/
    @Transactional
    public ResponseEntity<Object> expulsionTeammate(Long userId, Long postId, User user) {
        //추방하려는 유저정보 찾기
        User teammate = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER_INFO));
        //해당게식글 찾기
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        //모집글 작성자 확인
        if (!user.getId().equals(post.getUser().getId())) {
            throw new CustomException(ErrorCode.MYPAGE_INQUIRY_NO_AUTHORITY);
        }
        //수락정보 존재 확인
        acceptanceRepository.findByUserAndPost(teammate, post)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCEPTANCE_NOT_FOUND));
        //추방하려는 유저,게시글 객체로 찾아 삭제
        acceptanceRepository.deleteByUserAndPost(teammate, post);
        //현재모집인원 -1
        post.decreaseCnt();
        //모집인원 수 체크후 최대모집인원보다 현재모집인원이 적을경우 모집 중 으로 변경
        Boolean deadline = false;
        if (post.getCurrentMember() < post.getMaxCapacity()) {
            post.updateDeadline(deadline);
        }

        //지원자한테 알람 가야함
        //해당 댓글로 이동하는 url
        String Url = "https://dogpaw.kr/user/mypage/apply";
        String content = teammate.getNickname()+"님! 프로젝트 하차 알림이 도착했어요!";
        notificationService.send(teammate,NotificationType.REJECT,content,Url);

        return new ResponseEntity(new StatusResponseDto(teammate.getNickname()+"님 추방이 완료되었습니다",""), HttpStatus.OK);
    }
    @Transactional
    /*참가자 자진 팀 탈퇴*/
    public ResponseEntity<Object> withdrawTeam(Long postId, User user) {
        //해당게식글 찾기
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new CustomException(ErrorCode.POST_NOT_FOUND));
//        //해당유저의게시글과포스트찾기
//        UserApplication userApply = userApplicationRepository.findUserApplyByUserAndPost(user, post).orElseThrow(
//                () -> new CustomException(ErrorCode.APPLY_NOT_FOUND)
//        );
        //수락정보 존재 확인
        acceptanceRepository.findByUserAndPost(user,post)
                .orElseThrow(()-> new CustomException(ErrorCode.ACCEPTANCE_NOT_FOUND));
        //참여수락된 상태 db에서 삭제
        acceptanceRepository.deleteByUserAndPost(user,post);
        //현재모집인원 -1
        post.decreaseCnt();
        //모집인원 수 체크후 최대모집인원보다 현재모집인원이 적을경우 모집 중 으로 변경
        Boolean deadline = false;
        if(post.getCurrentMember()<post.getMaxCapacity()){
            post.updateDeadline(deadline);
        }
        // 스테이터스는 진행중으로 유지한다.

        //작성자에게 알람
        // '모집글' -> '신청' 시에 모집글 작성자에게 실시간 알림을 보낸다.
        //해당 댓글로 이동하는 url
        String Url = "https://dogpaw.kr/detail/"+post.getId();
        //탈퇴 시 모집글 작성 유저에게 실시간 알림 전송 ,
        String notificationContent = user.getNickname()+"님이 프로젝트를 하차하였습니다";
        notificationService.send(post.getUser(), NotificationType.REJECT,notificationContent,Url);

        return new ResponseEntity(new StatusResponseDto("팀 탈퇴가 완료되었습니다",""), HttpStatus.OK);
    }

    /*다른유저 마이페이지 보기(프로필,참여한 프로젝트, 모집중인 프로젝트)*/
    public OtherUserMypageResponseDto mypageInfo(String nickname, User user) {
        User otherUser = userRepository.findByNickname(nickname)
                .orElseThrow(()->new CustomException(ErrorCode.NOT_FOUND_USER_INFO));

        //반환할 ArrayList 생성

        /*다른유저의 프로필정보*/
        UserInfo userInfo = new UserInfo(otherUser.getUsername(), otherUser.getNickname(),otherUser.getProfileImg(), otherUser.getStacks());

        /*다른유저의 참여중인 프로젝트 리스트*/
        //다른 유저의 참여완료된(수락된) 리스트
        List<Acceptance> acceptances = acceptanceRepository.findAllByUserOrderByIdDesc(otherUser);
        //현재 로그인한 유저의 북마크 리스트
        List<BookMark> bookMarks = bookMarkRepository.findAllByUser(user);
        //게시물 객체를 담아줄 ArrayList 생성
        ArrayList<MyAcceptanceResponseDto> acceptancePostList = new ArrayList<>();
        ArrayList<Post> acceptedList = new ArrayList<>();
        HashSet<Long> bookMarkedList = new HashSet<>();

        Boolean bookMarkStatus = false;

        //해당 유저의 참여완료된 모집글 객체를 하나씩 ArrayList에 담아줌
        acceptances.forEach(ac->acceptedList.add(ac.getPost()));

        //로그인한 유저가 북마크한 모집글 객체를 하나씩 ArrayList에 담아줌
        bookMarks.forEach(bm-> bookMarkedList.add(bm.getPost().getId()));

        for(Post acceptedPost : acceptedList){
            //북마크 여부
            bookMarkStatus = bookMarkedList.contains(acceptedPost.getId());
            //기술스택
            List<PostStack> postStacks = acceptedPost.getPostStacks();
            List<String> stringPostStacks = new ArrayList<>();
            postStacks.forEach(ps->stringPostStacks.add(ps.getStack()));

            MyAcceptanceResponseDto acceptanceDto = new MyAcceptanceResponseDto(acceptedPost, stringPostStacks, bookMarkStatus, acceptedPost.getUser());
            acceptancePostList.add(acceptanceDto);
        }

        /*다른유저의 작성글 리스트*/
        //다른 유저가 작성한 모든글 리스트로 불러옴///(모든 게시글 X)
        List<Post> posts = postRepository.findAllByUserOrderByIdDesc(otherUser);

        ArrayList<MyPostResponseDto> postList = new ArrayList<>();

        //일치하면 bookMarkStatus = true 아니면 false를 bookMarkStatus에 담아줌
        for (Post post : posts) {
            //북마크 여부
            bookMarkStatus = bookMarkedList.contains(post.getId());
            List<PostStack> postStacks = post.getPostStacks();
            List<String> stringPostStacks = new ArrayList<>();
            postStacks.forEach(ps->stringPostStacks.add(ps.getStack()));

            MyPostResponseDto postDto = new MyPostResponseDto(post, stringPostStacks, bookMarkStatus, post.getUser());
            postList.add(postDto);
        }
        return new OtherUserMypageResponseDto(userInfo,acceptancePostList,postList);
    }
}
