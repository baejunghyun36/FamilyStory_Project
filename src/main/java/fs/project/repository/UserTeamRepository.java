package fs.project.repository;

import fs.project.domain.Team;
import fs.project.domain.User;
import fs.project.domain.UserTeam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;


@Slf4j
@Repository
public class UserTeamRepository {

    @PersistenceContext
    private EntityManager em;

    //user 전체 조회.
    public List<UserTeam> findAll(){
        List<UserTeam> result = em.createQuery("select ut from UserTeam ut", UserTeam.class).getResultList();
        return result;
    }

    public void dropUserTeam(Long uid, Long tid) {

        //UserTeam으로부터 삭제를 하는데 삭제를 하는 조건은 UserTeam의 tID(현재 로그인된 세션의 tID)이면서
        //UserTeam의 user의 uID(현재 로그인된 세션의 uid)인것을 삭제한다.
        // 유저팀에서 나를 삭제
        String s = "delete from UserTeam ut where ut.team.tID = :tid and ut.user.uID = :uid";
        em.createQuery(s).setParameter("tid",tid).setParameter("uid", uid).executeUpdate();

      /*user의 main_tid가삭제할 user_team 의 tid와 같다면 user가 속한 uid값을 들고 있는
        user_team의 uid가 일치하는 값이 하나라도 존재한다면 그걸 main_tid로 둔다.
        */
        // 나
        User u = em.find(User.class, uid);
        if(u.getMainTid()==tid){ //
            List<UserTeam> mainTeamChange = findAll();
            boolean check =false;
            for (UserTeam mtc : mainTeamChange) {
                if (mtc.getUser().getUID()==uid) { // 다른팀을 나의 메인팀으로 바꿔줘
                    String s1 = "update User u set u.mainTid = :tid where u.uID = :uid";
                    //Team의 boss를 찾은 uid 값을 넣는다.
                    em.createQuery(s1).setParameter("tid",mtc.getTeam().getTID()).setParameter("uid", uid).executeUpdate();
                    check=true;
                    break;
                }
            }
            if(check==false){ // 다른팀이 없으면 내 메인팀은 널이야.
                String s2 = "update User u set u.mainTid = :tid where u.uID = :uid";
                //Team의 boss를 찾은 uid 값을 넣는다.
                em.createQuery(s2).setParameter("tid",null).setParameter("uid", uid).executeUpdate();
            }
        }
        log.info("----------------------durldurldrlul--------------");
    }
    public Team findTeam(Long tid){
        return em.find(Team.class, tid);
    }


    public boolean findDropTeam(Long tid) { //유저팀에 새 보스 준다.
        List<UserTeam> all = findAll(); //all 이라는 객체에 리스트 형식으로 유저 전체를 찾아서 치환
        for (UserTeam ut : all) {
            if (ut.getTeam().getTID().equals(tid)&&ut.isJoinUs()==true) { // 현재 가입요청을 제외한 팀 사람들만!
                                                                          //유저팀의 팀의 사람의 TID와 로그인된 TID가 같으면서 joinus가 트루라면

                //Team의 boss를 찾은 uid 값을 넣는다.
                String s = "update Team t set t.boss = :uid where t.tID = :tid";

                //uid라는 이름을 가진 곳에(위의 :uid) UserTeam테이블의 user의 UID와 그룹원의 tid에 업데이트 실행한다.
                em.createQuery(s).setParameter("uid",ut.getUser().getUID()).setParameter("tid", tid).executeUpdate();

                return true; //팀 테이블 지울 필요 없어
            }
        }
        return false; //팀 테이블을 지워
    }


    public void dropTeam(Long tid) { //팀 삭제
        List<UserTeam> all = findAll();
        for (UserTeam ut : all) {
            //만약 userteam의 tid와 tid의 값이 같으면서 userteam의 join_us의 값이 false 라면(그룹생성이 안되어 있다면),
            if (ut.getTeam().getTID().equals(tid)&&ut.isJoinUs()==false) {

                //Team의 boss를 찾은 uid 값을 넣는다.
                String s1 = "delete from UserTeam ut where ut.team.tID = :tid";
                em.createQuery(s1).setParameter("tid",tid).executeUpdate();
            }
        }
        //team테이블 삭제
        String s = "delete from Team t where t.tID = :tid ";
        em.createQuery(s).setParameter("tid",tid).executeUpdate();

        //만약 팀이 사라졌다면 가입요청한 사람들 모두 지우기

    }
}
