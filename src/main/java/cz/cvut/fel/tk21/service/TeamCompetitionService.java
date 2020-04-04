package cz.cvut.fel.tk21.service;

import cz.cvut.fel.tk21.dao.TeamCompetitionDao;
import cz.cvut.fel.tk21.model.Club;
import cz.cvut.fel.tk21.model.teams.Match;
import cz.cvut.fel.tk21.model.teams.Team;
import cz.cvut.fel.tk21.model.teams.TeamCompetition;
import cz.cvut.fel.tk21.rest.dto.teams.CompetitionDto;
import cz.cvut.fel.tk21.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamCompetitionService extends BaseService<TeamCompetitionDao, TeamCompetition> {

    private final TeamService teamService;
    private final MatchService matchService;

    @Autowired
    protected TeamCompetitionService(TeamCompetitionDao dao, TeamService teamService, MatchService matchService) {
        super(dao);
        this.teamService = teamService;
        this.matchService = matchService;
    }

    public List<CompetitionDto> getAllTeamCompetitionsInCurrentYear(Club club){
        int year = DateUtils.getCurrentYear();
        List<Team> teams = teamService.findAllTeamsInYearByClub(club, year);
        Map<TeamCompetition, List<Team>> map = new HashMap<>();
        for (Team t : teams){
            map.put(t.getCompetition(), new ArrayList<>());
        }
        for (Team t : teams){
            map.get(t.getCompetition()).add(t);
        }

        List<CompetitionDto> result = new ArrayList<>();
        for (Map.Entry<TeamCompetition, List<Team>> entry : map.entrySet()) {
            Map<Team, List<Match>> homeMatches = new HashMap<>();
            Map<Team, List<Match>> awayMatches = new HashMap<>();
            for (Team team : entry.getValue()){
                homeMatches.put(team, matchService.findHomeMatchesByTeam(team));
                awayMatches.put(team, matchService.findAwayMatchesByTeam(team));
            }
            result.add(new CompetitionDto(entry.getKey(), entry.getValue(), homeMatches, awayMatches));
        }

        return result;
    }

}
