package story.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import common.JDBCTemplate;
import story.model.vo.Story;
import story.model.vo.StoryFile;
import story.model.vo.StoryReply;

public class StoryDAO {

	public int insertStory(Connection conn, Story story) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		int result=0;
		String query ="SELECT SEQ_STORY.NEXTVAL AS SEQ_STORY FROM DUAL";
		String sql ="INSERT INTO STORY VALUES(?,?,?,DEFAULT,?,DEFAULT,DEFAULT,DEFAULT,?)";
		try {
			pstmt = conn.prepareStatement(query);
			rset = pstmt.executeQuery();
			int seqStory =Integer.MIN_VALUE;
			
			while(rset.next()) {
				seqStory=rset.getInt("SEQ_STORY");
			}
			pstmt=conn.prepareStatement(sql);
			pstmt.setInt(1,seqStory);
			pstmt.setString(2,story.getStoryContents()); //스토리 내용
			pstmt.setString(3,story.getStoryTag()); // 스토리 태그
			pstmt.setString(4,story.getUserId());
			pstmt.setString(5,story.getFileNo()); //첨부파일
			pstmt.executeUpdate();
			result=seqStory;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCTemplate.close(pstmt);
		}
		
		return result;
	}

	public int insertFile(Connection conn, StoryFile storyFile) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		int result = 0;
		String query="SELECT SEQ_STORY_FILE.NEXTVAL AS FILE_NO from DUAL";
		String sql="INSERT INTO STORY_FILE VALUES(?,?,?,?)";
		try {
			pstmt=conn.prepareStatement(query);
			rset = pstmt.executeQuery();
			int fileNo = Integer.MIN_VALUE;
			while(rset.next()) {
				fileNo=rset.getInt("FILE_NO");
			}
			result =fileNo;
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, fileNo);
			pstmt.setString(2,storyFile.getFilePath());
			pstmt.setString(3, storyFile.getFileName());
			pstmt.setLong(4, storyFile.getFileSize());
			pstmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			
		}
		return result;
	}

	public List<Story> selectAllStory(Connection conn, int currentPage) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		List<Story> sList = null;
		String sql="SELECT * FROM(SELECT ROW_NUMBER() OVER(ORDER BY STORY_NO DESC)AS NUM, STORY_NO,USER_ID,STORY_CONTENTS,STORYFILE_NAME,STORY_TAG,STORY_REPLYCOUNT,STORY_LIKE_COUNT,STORY_VIEWCOUNT FROM STORY S, STORY_FILE F WHERE S.FILE_NO = F.STORYFILE_NO) WHERE NUM BETWEEN ? AND ?";
		try {
			pstmt = conn.prepareStatement(sql);
			int viewCountPage = 12; //페이지 당 보여줄 게시글 갯수
			int start = currentPage * viewCountPage - (viewCountPage -1);
			int  end = currentPage * viewCountPage;
			pstmt.setInt(1, start);
			pstmt.setInt(2, end);
			rset = pstmt.executeQuery();
			sList = new ArrayList<Story>();
			while(rset.next()) {
				Story story = new Story();
				story.setStoryNo(rset.getInt("STORY_NO"));
				story.setUserId(rset.getString("USER_ID"));
				story.setStoryContents(rset.getString("STORY_CONTENTS"));
				story.setFileName(rset.getString("STORYFILE_NAME"));
				story.setStoryTag(rset.getString("STORY_TAG"));
				story.setStoryReplyCount(rset.getInt("STORY_REPLYCOUNT"));
				story.setStoryLikeCount(rset.getInt("STORY_LIKE_COUNT"));
				story.setStoryViewCount(rset.getInt("STORY_VIEWCOUNT"));
				sList.add(story);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCTemplate.close(pstmt);
			JDBCTemplate.close(rset);
		}
		
		return sList;
	}

	public String getPageNavi(Connection conn, int currentPage) {
		int pageCountPerView = 5;
		int viewTotalCount = totalCount(conn);
		int viewCountPage = 12;
		int pageTotalCount = 0;
		
		int pageTotalCountMod = viewTotalCount % viewCountPage; //
		if(pageTotalCountMod > 0) {
			pageTotalCount = viewTotalCount/viewCountPage+1;
		}else {
			pageTotalCount = viewTotalCount/viewCountPage;
		}
		int startNavi=((currentPage-1)/pageCountPerView)*pageCountPerView+1;
		int endNavi = startNavi + pageCountPerView - 1;
		
		//게시물이 있는 페이지까지만 보여줘야함
		if(endNavi >pageTotalCount) {
			endNavi = pageTotalCount;
		}
		
		boolean needPrev = true;
		boolean needNext = true;
		if(startNavi == 1) {
			needPrev = false;
		}
		if(endNavi == pageTotalCount) {
			needNext = false;
		}
		StringBuilder sb = new StringBuilder();
		if(needPrev) {
			sb.append("<a href = '/story/list?currentPage="+(startNavi - 1)+"'>[이전]</a>");
		}
		for(int i = startNavi; i<=endNavi; i++) {
			if(i==currentPage) {
				sb.append(i);
			}else {
				sb.append("<a href='/story/list?currentPage=" + i + "'>" + i + "</a>");
			}
		}
		if(needNext) {
			sb.append("<a href='/recipe/list?currentPage=" + (endNavi + 1) + "'>[다음]</a>");
		}
		return sb.toString();
	}

	private int totalCount(Connection conn) {
		int totalValue = 0;
		Statement stmt = null;
		ResultSet rset = null;
		String sql ="SELECT COUNT(*) AS TOTALCOUNT FROM STORY";
		try {
			stmt = conn.createStatement();
			rset = stmt.executeQuery(sql);
			if(rset.next()) {
				totalValue = rset.getInt("TOTALCOUNT");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCTemplate.close(stmt);
			JDBCTemplate.close(rset);
		}
		return totalValue;
	}
	//스토리 상세
	public Story selectOneStroy(Connection conn, int storyNo) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		String sql ="SELECT STORY_NO,USER_ID,STORY_CONTENTS,FILE_NO,STORYFILE_NAME,STORY_TAG,STORY_VIEWCOUNT,STORY_ENROLLDATE,STORY_REPLYCOUNT,STORY_LIKE_COUNT FROM STORY S JOIN STORY_FILE F ON S.FILE_NO = F.STORYFILE_NO WHERE STORY_NO = ?";
		Story storyOne = null;
		
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, storyNo);
			rset = pstmt.executeQuery();
			while(rset.next()) {
				storyOne = new Story();
				storyOne.setStoryNo(storyNo);
				storyOne.setUserId(rset.getString("USER_ID"));
				storyOne.setStoryContents(rset.getString("STORY_CONTENTS"));
				storyOne.setFileName(rset.getString("STORYFILE_NAME"));
				storyOne.setStoryTag(rset.getString("STORY_TAG"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return storyOne;
	}
	//스토리 댓글 등록
	public int insertStoryReply(Connection conn, String userId, int storyNo, String replyContents,
			Timestamp uploadTime) {
		PreparedStatement pstmt = null;
		int result = 0;
		String sql="INSERT INTO STORY_REPLY VALUES(SEQ_STORY_REPLY.NEXTVAL,?,?,?,?)";
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, storyNo);
			pstmt.setString(2, userId);
			pstmt.setString(3, replyContents);
			pstmt.setTimestamp(4, uploadTime);
			result=pstmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}
	//상세 댓글 리스트
	public List<StoryReply> selectAllStoryReply(Connection conn, int storyNo) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		List<StoryReply> List = null;
		StoryReply storyReply = null;
		String sql ="SELECT * FROM STORY_REPLY WHERE STORY_NO = ? ORDER BY 1 DESC";
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, storyNo);
			rset = pstmt.executeQuery();
			List = new ArrayList();
			while(rset.next()) {
				storyReply = new StoryReply();
				storyReply.setReplyNo(rset.getInt("REPLY_NO"));
				storyReply.setStoryNo(rset.getInt("STORY_NO"));
				storyReply.setReplyUserId(rset.getString("REPLY_NICKNAME"));
				storyReply.setReplyContents(rset.getString("REPLY_CONTENTS"));
				storyReply.setReplyDate(rset.getTimestamp("REPLY_ENROLLDATE"));
				List.add(storyReply);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return List;
	}

	
}
