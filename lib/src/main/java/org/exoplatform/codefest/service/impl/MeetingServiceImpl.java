package org.exoplatform.codefest.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.codefest.service.MeetingService;
import org.exoplatform.codefest.entity.Meeting;
import org.exoplatform.codefest.entity.Page;
import org.exoplatform.codefest.entity.TimeOption;
import org.exoplatform.codefest.entity.UserVoted;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/6/15
 * Code-fest Team-A 2015
 * Implement of Meeting Service
 */
public class MeetingServiceImpl implements MeetingService {

  private Log log = ExoLogger.getExoLogger(MeetingService.class);
  private String repo = "repository";
  private String ws = "collaboration";

  private RepositoryService repoService;
  private SessionProviderService sessionProviderService;
  private Gson gson;


  public MeetingServiceImpl(RepositoryService repoService,
                            SessionProviderService sessionProviderService) {
    this.repoService = repoService;
    this.sessionProviderService = sessionProviderService;
    this.gson = new Gson();
//    try {
//      this.repo = repoService.getCurrentRepository().getConfiguration().getName();
//    } catch (Exception ex) {
//      if (log.isErrorEnabled()) {
//        log.error("Using default repository & workspace", ex.getMessage());
//      }
//    }

  }


  @Override
  public List<Meeting> getMeetings(String username, Page page) {
    return getMeetings(username, 1, page);
  }

  @Override
  public Meeting getMeeting(String jcrPath) throws Exception{
    Session session = getSession();
    Node meetingNode = (Node)session.getItem(jcrPath);
    Meeting meeting = new Meeting();
    meetingNodeToObject(meeting, meetingNode);
    return meeting;
  }

  @Override
  public long getMeetingTotal(String username) {
    try {
      Session session = getSession();
      StringBuilder queryBuilder = new StringBuilder("SELECT * FROM ").append(EXO_MEETING);
      queryBuilder.append(" WHERE jcr:path LIKE '/" + EXO_MEETING_DRIVE + "/%' ");
      queryBuilder.append(" AND (" + EXO_PROP_MEETING_OWNER + " LIKE '%" + username + "%'");
      queryBuilder.append(" OR CONTAINS(" + EXO_PROP_MEETING_PARTICIPANT + ", '" + username + "')");
      queryBuilder.append(") ");
      queryBuilder.append(" ORDER BY " + EXO_PROP_MEETING_DATE_CREATED + " DESC ");
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(queryBuilder.toString(), Query.SQL);
      NodeIterator nodes = query.execute().getNodes();
      return nodes.getSize();
    } catch (Exception ex) {
      if (log.isErrorEnabled()) log.error(ex.getMessage());
    }
    return 0;
  }

  @Override
  public Meeting save(Meeting meeting) throws Exception {
    Session session = getSession();
    Node rootNode = session.getRootNode();
    Calendar calendar = Calendar.getInstance();
    String meetingYear = String.valueOf(calendar.get(Calendar.YEAR));
    String meetingMonth = String.valueOf(calendar.get(Calendar.MONTH));

    if (!rootNode.hasNode(EXO_MEETING_DRIVE)) {
      rootNode.addNode(EXO_MEETING_DRIVE);
      rootNode.save();
    }
    Node meetingDrive = rootNode.getNode(EXO_MEETING_DRIVE);

    if (!meetingDrive.hasNode(meetingYear)) {
      meetingDrive.addNode(meetingYear);
      meetingDrive.save();
    }
    Node meetingYearNode = meetingDrive.getNode(meetingYear);
    if (!meetingYearNode.hasNode(meetingMonth)) {
      meetingYearNode.addNode(meetingMonth);
      meetingYearNode.save();
    }

    Node meetingMonthNode = meetingYearNode.getNode(meetingMonth);

    if(!meetingMonthNode.hasNode(meeting.getId())) meetingMonthNode.addNode(meeting.getId(), EXO_MEETING);

    Node meetingNode = meetingMonthNode.getNode(meeting.getId());
    meetingNode.setProperty(EXO_PROP_MEETING_TITLE, meeting.getTitle());
    meetingNode.setProperty(EXO_PROP_MEETING_DESCRIPTION, meeting.getDescription());
    meetingNode.setProperty(EXO_PROP_MEETING_VALIDATION, meeting.getMeetingValidation());
    meetingNode.setProperty(EXO_PROP_MEETING_TIME_OPTION, gson.toJson(meeting.getTimeOptions()));
    meetingNode.setProperty(EXO_PROP_MEETING_DOC_PATH, meeting.getDocumentPath());
    meetingNode.setProperty(EXO_PROP_MEETING_LOCATION, meeting.getLocation());
    meetingNode.setProperty(EXO_PROP_MEETING_OWNER, meeting.getOwner());
    meetingNode.setProperty(EXO_PROP_MEETING_PARTICIPANT, gson.toJson(meeting.getParticipant()));
    meetingNode.setProperty(EXO_PROP_MEETING_USER_VOTED, gson.toJson(meeting.getUserVotes()));
    meetingNode.setProperty(EXO_PROP_MEETING_STATUS, meeting.getStatus());
    meetingNode.setProperty(EXO_PROP_MEETING_TYPE, meeting.getType());
    meetingNode.setProperty(EXO_PROP_MEETING_DATE_CREATED, meeting.getDateCreated());
    meetingNode.setProperty(EXO_PROP_MEETING_DATE_MODIFIED, meeting.getDateModified());
    meeting.setJcrPath(meetingNode.getPath());
    meetingMonthNode.save();
    return meeting;
  }

  @Override
  public boolean delete(Meeting meeting) throws Exception{
    Session session = getSession();
    Node m = (Node) session.getItem(meeting.getJcrPath());
    m.remove();
    m.getParent().save();
    return true;
  }

  @Override
  public List<Meeting> getMeetings(String username, int status, Page page) {
    try {
      Session session = getSession();
      List<Meeting> meetings = new ArrayList<Meeting>();
      StringBuilder queryBuilder = new StringBuilder("SELECT * FROM ").append(EXO_MEETING);
      queryBuilder.append(" WHERE jcr:path LIKE '/" + EXO_MEETING_DRIVE + "/%' ");
      queryBuilder.append(" AND (" + EXO_PROP_MEETING_OWNER + " LIKE '%" + username + "%'");
      queryBuilder.append(" OR CONTAINS(" + EXO_PROP_MEETING_PARTICIPANT + ", '" + username + "')");
      queryBuilder.append(") ");
      queryBuilder.append(" AND (" + EXO_PROP_MEETING_STATUS + " = '" + status + "' )");
      String sortCriterion = "";
      if (page != null) sortCriterion = page.getSort();
      if (StringUtils.isEmpty(sortCriterion)) sortCriterion = "ASC";
      queryBuilder.append(" ORDER BY " + EXO_PROP_MEETING_DATE_CREATED + " " + sortCriterion);
      log.info("Query to getMeetings: " + queryBuilder.toString());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      QueryImpl query = (QueryImpl) queryManager.createQuery(queryBuilder.toString(), Query.SQL);
      if (page != null) {
        query.setLimit(page.getLimit());
        query.setOffset(page.getOffset());
      }
      NodeIterator nodes = query.execute().getNodes();
      while (nodes.hasNext()) {
        Meeting meeting = new Meeting();
        Node node = nodes.nextNode();
        meetingNodeToObject(meeting, node);
        meetings.add(meeting);
      }
      return meetings;
    } catch (Exception ex) {
      log.error("Exception in getMeetings.", ex);
    }
    return null;
  }

  @Override
  public Meeting updateVote(Meeting m, String username, Map<String, String> userVoteds) throws Exception{
    Meeting meeting = getMeeting(m.getJcrPath());
    List<UserVoted> _userVotedsFromDB = meeting.getUserVotes();
    if(_userVotedsFromDB == null ) _userVotedsFromDB = new ArrayList<UserVoted>();


    for (String optionId: userVoteds.keySet()) {// optionids to update
      UserVoted userVoted = null;
      for(UserVoted v : _userVotedsFromDB) {
        if (v.getUsername() == username && v.getOptionId() == optionId) {
          userVoted = v;
          break;
        }
      }
      if (userVoted == null) {
        userVoted = new UserVoted(username, optionId, Integer.parseInt(userVoteds.get(optionId)));
        _userVotedsFromDB.add(userVoted);
      } else {
        userVoted.setValue(Integer.parseInt(userVoteds.get(optionId)));
      }
    }

    meeting.setUserVotes(_userVotedsFromDB);
    return save(meeting);
  }

  @Override
  public Meeting finalMeeting(Meeting meeting, List<String> timeOptionIds) throws Exception{
    meeting.setStatus(1); //close voting
    List<TimeOption> timeOptions = meeting.getTimeOptions();
    for (TimeOption timeOption: timeOptions){
      if(timeOptionIds.contains(timeOption.getId())){
        timeOption.setSelected(true);
      }
    }
    return save(meeting);
  }

  /**
   * Get system session, only for init data.
   * Please NOT use for navigate JCR data
   *
   * @return
   * @throws Exception
   */
  private Session getSession() throws Exception {
    ManageableRepository repository = repoService.getRepository(this.repo);
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    Session session = sessionProvider.getSession(this.ws, repository);
    return session;
  }

  @Override
  public Meeting addParticipant(Meeting meeting, String username) throws Exception{
    Session session = getSession();
    List<String> participants = new ArrayList<String>();
    Type listTypeOfString = new TypeToken<List<String>>() {}.getType();
    Node meetingNode = (Node) session.getItem(meeting.getJcrPath());
    if (meetingNode.hasProperty(EXO_PROP_MEETING_PARTICIPANT)) {
      String participantOrigin = meetingNode.getProperty(EXO_PROP_MEETING_PARTICIPANT).getString();
      participants = gson.fromJson(participantOrigin, listTypeOfString);
      if(!participants.contains(username)) participants.add(username);
      meetingNode.setProperty(EXO_PROP_MEETING_PARTICIPANT, gson.toJson(participants));
      meetingNode.getParent().save();
      meeting.setParticipant(participants);
      return meeting;
    }
    return null;
  }

  @Override
  public Meeting removeParticipant(Meeting meeting, String username) throws Exception {
    Session session = getSession();
    List<String> participants = new ArrayList<String>();
    Type listTypeOfString = new TypeToken<List<String>>() {}.getType();
    Node meetingNode = (Node) session.getItem(meeting.getJcrPath());
    if (meetingNode.hasProperty(EXO_PROP_MEETING_PARTICIPANT)) {
      String participantOrigin = meetingNode.getProperty(EXO_PROP_MEETING_PARTICIPANT).getString();
      participants = gson.fromJson(participantOrigin, listTypeOfString);
      if(participants.contains(username)) participants.remove(username);
      meetingNode.setProperty(EXO_PROP_MEETING_PARTICIPANT, gson.toJson(participants));
      meetingNode.getParent().save();
      meeting.setParticipant(participants);
      return meeting;
    }
    return null;
  }

  private void meetingNodeToObject(Meeting meeting, Node node) throws Exception{
    List<TimeOption> timeOptions = new ArrayList<TimeOption>();
    List<String> participants = new ArrayList<String>();
    List<UserVoted> userVotes = new ArrayList<UserVoted>();

    Type listTypeOfString = new TypeToken<List<String>>() {}.getType();
    Type listTypeOfTimeOption = new TypeToken<List<TimeOption>>() {}.getType();
    Type listTypeOfUserVote = new TypeToken<List<UserVoted>>() {}.getType();

    if (node.hasProperty(EXO_PROP_MEETING_TITLE))
      meeting.setTitle(node.getProperty(EXO_PROP_MEETING_TITLE).getString());
    if (node.hasProperty(EXO_PROP_MEETING_DESCRIPTION))
      meeting.setDescription(node.getProperty(EXO_PROP_MEETING_DESCRIPTION).getString());
    if (node.hasProperty(EXO_PROP_MEETING_LOCATION))
      meeting.setLocation(node.getProperty(EXO_PROP_MEETING_LOCATION).getString());
    if (node.hasProperty(EXO_PROP_MEETING_TYPE))
      meeting.setType(node.getProperty(EXO_PROP_MEETING_TYPE).getString());
    if (node.hasProperty(EXO_PROP_MEETING_STATUS))
      meeting.setStatus(Integer.parseInt(node.getProperty(EXO_PROP_MEETING_STATUS).getString()));
    if (node.hasProperty(EXO_PROP_MEETING_OWNER))
      meeting.setOwner(node.getProperty(EXO_PROP_MEETING_OWNER).getString());
    if (node.hasProperty(EXO_PROP_MEETING_DOC_PATH))
      meeting.setDocumentPath(node.getProperty(EXO_PROP_MEETING_DOC_PATH).getString());
    if (node.hasProperty(EXO_PROP_MEETING_VALIDATION))
      meeting.setMeetingValidation(node.getProperty(EXO_PROP_MEETING_VALIDATION).getLong());
    if (node.hasProperty(EXO_PROP_MEETING_PARTICIPANT)) {
      participants = gson.fromJson(node.getProperty(EXO_PROP_MEETING_PARTICIPANT).getString(), listTypeOfString);
      meeting.setParticipant(participants);
    }
    if (node.hasProperty(EXO_PROP_MEETING_TIME_OPTION)) {
      timeOptions = gson.fromJson(node.getProperty(EXO_PROP_MEETING_TIME_OPTION).getString(), listTypeOfTimeOption);
      meeting.setTimeOptions(timeOptions);
    }
    if (node.hasProperty(EXO_PROP_MEETING_USER_VOTED)) {
      userVotes = gson.fromJson(node.getProperty(EXO_PROP_MEETING_USER_VOTED).getString(), listTypeOfUserVote);
      meeting.setUserVotes(userVotes);
    }
    if (node.hasProperty(EXO_PROP_MEETING_DATE_CREATED))
      meeting.setDateCreated(node.getProperty(EXO_PROP_MEETING_DATE_CREATED).getLong());
    if (node.hasProperty(EXO_PROP_MEETING_DATE_MODIFIED))
      meeting.setDateModified(node.getProperty(EXO_PROP_MEETING_DATE_MODIFIED).getLong());
  }
}
