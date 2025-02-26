package vote.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import common.JDBCTemplate;
import recipe.model.vo.Recipe;
import vote.model.vo.RecipeCandidate;

public class VoteDAO {

	public List<RecipeCandidate> selectVoteRecipe(Connection conn,String userId) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		List<RecipeCandidate> cList = null;
		String query = "select A.candidate_no, recipe_title, recipe_no,file_name,user_id, (select count(*) from vote where candidate_no = A.candidate_no and vote_state='Y') as vote_ct, nvl((select candidate_no from vote where user_id = ? and vote_state='Y'), 0) as vote_at from recipe_candidate a join (select * from recipe join recipe_file using(file_no)) using(recipe_no)";
		
		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, userId);
			rset = pstmt.executeQuery();
			cList = new ArrayList<RecipeCandidate>();
			while(rset.next()) {
				RecipeCandidate recipeCandidate = new RecipeCandidate();
				recipeCandidate.setCandidateNo(rset.getInt("candidate_no"));
				recipeCandidate.setRecipeTitle(rset.getString("recipe_title"));
				recipeCandidate.setRecipeNo(rset.getInt("recipe_no"));
				recipeCandidate.setFileName(rset.getString("file_name"));
				recipeCandidate.setRecipeUserId(rset.getString("user_id"));
				recipeCandidate.setVoteCount(rset.getInt("vote_ct"));
				recipeCandidate.setVoteAt(rset.getInt("vote_at"));
				cList.add(recipeCandidate);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return cList;
	}

	public List<Recipe> selectCandidateRecipe(Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		List<Recipe> rList = null;
		String query = "select rank() over (order by nvl(recipe_viewcount+\"like_count1\",0) desc)as\"rank\",\"like_count1\",recipe_viewcount,recipe_no,recipe_title, user_id, recipe_enrolldate,candidate_no from (select nvl(recipe_viewcount+\"like_count\",0)as\"likeview_sum\", recipe_no,recipe_title, nvl(\"like_count\",0) as \"like_count1\", recipe_viewcount, user_id, recipe_enrolldate from (select * from recipe left outer join (select count(*) as \"like_count\", recipe_NO from recipe_like group by recipe_no) using(recipe_no)) where recipe_legendstate = 0) left outer join recipe_candidate using(recipe_no) where candidate_no is null and ROWNUM<11";
		
		try {
			pstmt = conn.prepareStatement(query);
			rset = pstmt.executeQuery();
			rList = new ArrayList<Recipe>();
			while(rset.next()) {
				Recipe recipe = new Recipe();
				recipe.setRecipeNo(rset.getInt("recipe_no"));
				recipe.setRecipeTitle(rset.getString("recipe_title"));
				recipe.setRecipeLikeCount(rset.getInt("like_count1"));
				recipe.setRecipeViewCount(rset.getInt("recipe_viewcount"));
				recipe.setUserId(rset.getString("user_id"));
				recipe.setRecipeEnrollDate(rset.getDate("recipe_enrolldate"));
				//좋아요수+조회수 토탈값도 쓸수 있음(컬럼명 : likeview_sum)
				//랭킹도 쓸수 있으면 (컬럼명 : rank)
				rList.add(recipe);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(rset);
			JDBCTemplate.close(pstmt);
		}
		return rList;
	}

	public int insertCandidate(Connection conn, String recipeNos) {
		int result = 0;
		PreparedStatement pstmt = null;
		String query ="insert into recipe_candidate values(seq_candidate.nextval,?,sysdate,default)";
		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, recipeNos);
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

	public int updateVoteState(Connection conn, String votingState) {
		int result = 0;
		PreparedStatement pstmt = null;
		String query = "update VOTING_STATUS set voting_state = ?";
		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, votingState);
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

	@SuppressWarnings("resource")
	public int updateLegendRecipeState(Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		int result = 0;
		String querySelect = "SELECT * FROM (select RANK() OVER (ORDER BY \"voteCount\" desc)as RANK,candidate_no, recipe_no, fixed_date,\"voteCount\" from (select candidate_no, recipe_no, fixed_date,nvl(\"득표수\",0) as\"voteCount\" from recipe_candidate left OUTER join (select count(*) as\"득표수\", candidate_no from vote group by candidate_no) using(candidate_no))) where RANK = 1";
		String query = "update recipe set recipe_legendState =1, legend_enrolldate = sysdate where recipe_no = ?";
		try {
			pstmt = conn.prepareStatement(querySelect);
			rset = pstmt.executeQuery();
			List<Integer> list = new ArrayList();
			while(rset.next()) {
				list.add(rset.getInt("recipe_no"));
			}
			
			pstmt = conn.prepareStatement(query);
			for(int i = 0; i<list.size();i++) {
				pstmt.setInt(1,list.get(i));
				result=pstmt.executeUpdate();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
			JDBCTemplate.close(rset);
		}
		return result;
	}

	public int deleteVoteCandidate(Connection conn) {
		PreparedStatement pstmt = null;
		int result = 0;
		String query = "DELETE FROM recipe_candidate";
		try {
			pstmt = conn.prepareStatement(query);
			
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

	//투표 진행 상태를 받아오기위한 메소드
	public String selectVotingState(Connection conn) {
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		String query = "select * from voting_status";
		String result = "";
		try {
			pstmt = conn.prepareStatement(query);
			rset=pstmt.executeQuery();
			while(rset.next()) {
				result = rset.getString("voting_state");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
			JDBCTemplate.close(rset);
		}
		
		return result;
	}

	public int deleteVoteAction(Connection conn, int candidateNo, String userId) {
		PreparedStatement pstmt = null;
		int result = 0;
		String query = "DELETE FROM vote WHERE candidate_NO=? and user_id=?";
		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, candidateNo);
			pstmt.setString(2, userId);
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

	public int insertVoteAction(Connection conn, int candidateNo, String userId) {
		PreparedStatement pstmt = null;
		int result = 0;
		String query = "insert into vote values(seq_vote.nextval,?,?,sysdate,default)";
		try {
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, userId);
			pstmt.setInt(2, candidateNo);
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

	public int updateUserVoteState(Connection conn) {
		int result = 0;
		PreparedStatement pstmt = null;
		String query = "update vote set vote_state = 'N'";
		try {
			pstmt = conn.prepareStatement(query);
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			JDBCTemplate.close(pstmt);
		}
		return result;
	}

}
