import twitter4j.*;
import twitter4j.auth.AccessToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeong on 2017-06-03.
 */
public class Gabolga extends Thread {
    static final String CONSUMER_KEY = Config.CONSUMER_KEY;
    static final String CONSUMER_SECRET = Config.CONSUMER_SECRET;
    static final String ACCESS_TOKEN = Config.ACCESS_TOKEN;
    static final String ACCESS_SECRET = Config.ACCESS_SECRET;

    static DBManager db = new DBManager();

    static Twitter twitter;
    DirectMessage dm;
    static Pattern urlPattern = Pattern.compile("^(https?):\\/\\/([^:\\/\\s]+)(:([^\\/]*))?((\\/[^\\s/\\/]+)*)?\\/([^#\\s\\?]*)(\\?([^#\\s]*))?(#(\\w*))?$");

    public Gabolga(DirectMessage dm) {
        this.dm = dm;
    }

    public static void main(String[] args) {
        String time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
        System.out.println(time + "START");

        // 마지막 트윗 아이디 불러오기
        long last_dm = db.dmCheck();
        if (last_dm == -1) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + "Last DM Read Error AND DIE ******");
            return;
        }

        /*File file = new File("gabolga_last_tweet_id");
        String last_tweet_id = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            last_tweet_id = reader.readLine();
            reader.close();
        } catch (Exception e) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + "File Read Error AND DIE ******");
            return;
        }*/

        ArrayList<Thread> threads = new ArrayList<Thread>();

        // 가볼가 계정 트위터 API 불러오기
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
        twitter.setOAuthAccessToken(new AccessToken(ACCESS_TOKEN, ACCESS_SECRET));

        // DM 불러오기
        Paging paging = new Paging(last_dm+1);
        ResponseList<DirectMessage> DMs = null;
        try {
            DMs = twitter.directMessages().getDirectMessages(paging);
        } catch (TwitterException e) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + "DM Read Error AND DIE: "+e.toString()+" ******");
            return;
        }

        if(DMs.size() < 1) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + "END");
            return;
        }

        // DM 별로 스레드 작동
        for(DirectMessage dm : DMs){
            Thread t = new Gabolga(dm);
            t.start();
            threads.add(t);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        for(int i=0; i<threads.size(); i++) {
            Thread t = threads.get(i);
            try {
                t.join();
            }catch(Exception e) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        // 마지막 트윗 아이디 저장
        /*last_tweet_id = Long.toString(DMs.get(DMs.size()-1).getId());
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));

            writer.write(last_tweet_id);
            writer.close();
        } catch (Exception e) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + "File Write Error AND DIE ******");
            return;
        }*/

        time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
        System.out.println(time + "END");
    }

    public void run() {
        String time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
        System.out.println(time + this.dm.getId() + " START");

        // URL 유무 확인
        URLEntity[] urls = this.dm.getURLEntities();
        if(urls.length == 0) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + this.dm.getId() + " have No URL AND END");

            db.insertDM(dm.getId(), dm.getSenderId(), 0);
            return;
        }

        // 트윗 URL 유무 확인
        String url = urls[0].getExpandedURL();
        Matcher mc = urlPattern.matcher(url);
        mc.matches();

        Long tweet_id;
        if(mc.group(2).equals("twitter.com") && mc.group(6).equals("/status")) {
            tweet_id = Long.parseLong(mc.group(7));
        } else {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + this.dm.getId() + " have No Tweet URL AND END");

            db.insertDM(dm.getId(), dm.getSenderId(), 0);
            return;
        }
        Long user_id = dm.getSenderId();

        db.insert(user_id, tweet_id);   // my_map 등록
        db.insertDM(dm.getId(), dm.getSenderId(), 1);

        String message = null;

        // 트윗 등록 체크
        if(db.tweetCheck(tweet_id)) {
            // 유저 등록 체크
            if(db.userCheck(user_id)) {
                // 트윗 등록 완료 DM
                message = "내 지도에 등록되었습니다. 확인해보세요! \nhttps://gabolga.gamjaa.com/my-map.php?tweet_id="+tweet_id;
            } else {
                // 유저 등록 권유 DM
                message = "보내주신 트윗에 등록된 장소입니다. \n가볼가에 가입하시면 내 지도에서 확인할 수 있어요! \nhttps://gabolga.gamjaa.com/tweet.php?tweet_id="+tweet_id;
            }
        } else {
            // 유저 등록 체크
            if(db.userCheck(user_id)) {
                // 트윗 등록 권유 DM
                message = "앗! 처음 보는 트윗이에요! \n직접 위치를 등록해주시면 안 될까요? ;^; \nhttps://gabolga.gamjaa.com/tweet.php?tweet_id="+tweet_id;
            } else {
                // 유저 등록 권유 DM
                message = "앗! 처음 보는 트윗이에요! \n가입하시면 직접 위치를 등록하실 수 있어요! \nhttps://gabolga.gamjaa.com/sign-in.php?url=tweet.php?tweet_id="+tweet_id;
            }
        }

        // DM 전송
        try {
            twitter.directMessages().sendDirectMessage(user_id, message);
        } catch (TwitterException e) {
            time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
            System.out.println(time + this.dm.getId() + "DM SEND Error AND END: "+e.toString()+" ******");
            return;
        }
        time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
        System.out.println(time + this.dm.getId() + " Send Message");

        // 종료
        time = (new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ").format(Calendar.getInstance().getTime()));
        System.out.println(time + this.dm.getId() + " END");
    }
}
