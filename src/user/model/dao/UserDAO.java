package user.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import common.JDBCTemplate;
import user.model.vo.User;

public class UserDAO {
	// 로그인
	public User selectLogin(Connection conn, String userId, String userPwd) {
		User user = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		String query = "SELECT * FROM USERS WHERE USER_ID = ? AND USER_PWD =?";

		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, userId);
			pstmt.setString(2, userPwd);
			rset = pstmt.executeQuery();
			if (rset.next()) {
				user = new User();
				user.setUserId(rset.getString("USER_ID"));
				user.setUserPwd(rset.getString("USER_PWD"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
			JDBCTemplate.close(rset);
		}
		return user;
	}

	// 회원가입
	public int insertUser(Connection conn, User user) {
		int result = 0;
		PreparedStatement pstmt = null;
		String query = "INSERT INTO USERS VALUES(SEQ_USER_NO.NEXTVAL,?,?,?,?,?,DEFAULT,DEFAULT,DEFAULT)";

		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, user.getUserId());
			pstmt.setString(2, user.getUserPwd());
			pstmt.setString(3, user.getUserEmail());
			pstmt.setString(4, user.getUserName());
			pstmt.setString(5, user.getUserPhone());
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

	// 아이디 찾기
	public User selectOneById(Connection conn, String userName, String userEmail) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		User userOne = null;
		String query = "SELECT USER_ID FROM USERS WHERE USER_NAME = ? AND USER_EMAIL = ?";
		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, userName);
			pstmt.setString(2, userEmail);
			rset = pstmt.executeQuery();
			if (rset.next()) {
				userOne = new User();
				userOne.setUserId(rset.getString("USER_ID"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
			JDBCTemplate.close(rset);
		}
		return userOne;
	}

	// 비밀번호 찾기
	public User selectOneByPwd(Connection conn, String userId, String userEmail) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		User userOne = null;
		String query = "SELECT USER_PWD FROM USERS WHERE USER_ID = ? AND USER_EMAIL = ?";

		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, userId);
			pstmt.setString(2, userEmail);
			rset = pstmt.executeQuery();
			if (rset.next()) {
				userOne = new User();
				userOne.setUserPwd(rset.getString("USER_PWD"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return userOne;
	}

	// 비밀번호 변경
	public int updateUserPwd(String userId, String authenticationKey, Connection conn) {
		PreparedStatement pstmt = null;
		int result = 0;
		String query = "UPDATE USERS SET USER_PWD = ? WHERE USER_ID = ?";

		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, authenticationKey);
			pstmt.setString(2, userId);
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}
	// 회원 리스트 전체 보기
	public List<User> selectAllUser(Connection conn, int currentPage) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		List<User> uList = null;
		String query = "SELECT * FROM(SELECT ROW_NUMBER() OVER(ORDER BY USER_NO ASC)"
				+ "AS NUM,USER_NO, USER_ID, USER_NAME, USER_EMAIL, USER_PHONE, USER_ADMIN_YN FROM USERS)"
				+ "WHERE USER_ADMIN_YN = 'N' AND NUM BETWEEN ? AND ?";
		
		try {
			pstmt = conn.prepareStatement(query);
			int viewCountPerPage = 10;
			int start = currentPage*viewCountPerPage-(viewCountPerPage-1);
			int end = currentPage*viewCountPerPage;
			
			pstmt.setInt(1, start);
			pstmt.setInt(2, end);
			rset = pstmt.executeQuery();
			uList = new ArrayList<User>();
			while(rset.next()) {
				User user = new User();
				user.setUserNo(rset.getInt("USER_NO"));
				user.setUserId(rset.getString("USER_ID"));
				user.setUserName(rset.getString("USER_NAME"));
				user.setUserEmail(rset.getString("USER_EMAIL"));
				user.setUserPhone(rset.getString("USER_PHONE"));
				user.setUserAdmin(rset.getString("USER_ADMIN_YN"));
				uList.add(user);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return uList;
	}
	// 페이징처리
	public String getPageNavi(Connection conn, int currentPage) {
		int pageCountPerView = 5;
		int viewTotalCount = totalCount(conn);
		int viewCountPerPage = 10;
		int pageTotalCount = 0;
		int pageTotalCountMod = viewTotalCount % viewCountPerPage;
		if(pageTotalCountMod > 0) {
			pageTotalCount = viewTotalCount / viewCountPerPage + 1;
		}else {
			pageTotalCount = viewTotalCount / viewCountPerPage;
		}
		int startNavi = ((currentPage-1)/pageCountPerView) * pageCountPerView + 1;
		int endNavi = startNavi + pageCountPerView - 1;
		if(endNavi > pageTotalCount) {
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
			sb.append("<a href='/user/list?currentPage=" + (startNavi-1) + "'> [이전] </a>");
		}
		for(int i=startNavi; i<=endNavi; i++) {
			if(i == currentPage) {
				sb.append(i + " ");
			}else {
				sb.append("<a href='/user/list?currentPage=" + i + "'>" + i + "</a>");
			}
		}
		if(needNext) {
			sb.append("<a href='/user/list?currentPage=" + (endNavi+1) + "'> [다음] </a>");
		}
		return sb.toString();
	}
	// 전체 회원 숫자를 세줄 메소드
	private int totalCount(Connection conn) {
		int totalValue = 0;
		Statement stmt = null;
		ResultSet rset = null;
		String query = "SELECT COUNT(*) AS TOTALCOUNT FROM USERS";
		
		try {
			stmt = conn.createStatement();
			rset = stmt.executeQuery(query);
			if(rset.next()) {
				totalValue = rset.getInt("TOTALCOUNT");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(stmt);
		}
		return totalValue;
	}

	public List<User> selectAllAdmin(Connection conn, int currentPage) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		List<User> aList = null;
		String query = "SELECT * FROM(SELECT ROW_NUMBER() OVER(ORDER BY USER_NO ASC)"
				+ "AS NUM,USER_NO, USER_ID, USER_NAME, USER_EMAIL, USER_PHONE, USER_ADMIN_YN FROM USERS)"
				+ "WHERE USER_ADMIN_YN = 'Y' AND NUM BETWEEN ? AND ?";
		
		try {
			pstmt = conn.prepareStatement(query);
			int viewCountPerPage = 10;
			int start = currentPage*viewCountPerPage-(viewCountPerPage-1);
			int end = currentPage*viewCountPerPage;
			
			pstmt.setInt(1, start);
			pstmt.setInt(2, end);
			rset = pstmt.executeQuery();
			aList = new ArrayList<User>();
			while(rset.next()) {
				User user = new User();
				user.setUserNo(rset.getInt("USER_NO"));
				user.setUserId(rset.getString("USER_ID"));
				user.setUserName(rset.getString("USER_NAME"));
				user.setUserEmail(rset.getString("USER_EMAIL"));
				user.setUserPhone(rset.getString("USER_PHONE"));
				user.setUserAdmin(rset.getString("USER_ADMIN_YN"));
				aList.add(user);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return aList;
	}
}
