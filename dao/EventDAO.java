package personalagenda.dao;

import personalagenda.model.Event;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {
    
    private Connection getConnection() throws SQLException {
        String url = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
        String user = "agenda_user";
        String password = "agenda123";
        return DriverManager.getConnection(url, user, password);
    }
    
    public void addEvent(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, event_date, event_time, description, event_type, importance_level) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getTitle());
            pstmt.setDate(2, Date.valueOf(event.getDate()));
            pstmt.setString(3, event.getTime() != null ? event.getTime().toString() : null);
            pstmt.setString(4, event.getDescription());
            pstmt.setString(5, event.getType());
            pstmt.setInt(6, event.getImportanceLevel());
            pstmt.executeUpdate();
        }
    }
    
    public List<Event> getAllEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events ORDER BY event_date, event_time";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public List<Event> searchByTitle(String keyword) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE UPPER(title) LIKE UPPER(?) ORDER BY event_date";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public List<Event> searchByCategory(String category) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE UPPER(event_type) LIKE UPPER(?) ORDER BY event_date";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + category + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public List<Event> searchByDate(LocalDate date) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE event_date = ? ORDER BY event_time";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, Date.valueOf(date));
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public List<Event> getImportantEvents() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events WHERE importance_level >= 4 ORDER BY importance_level DESC, event_date";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(mapResultSetToEvent(rs));
            }
        }
        return events;
    }
    
    public void deleteEvent(int id) throws SQLException {
        String sql = "DELETE FROM events WHERE event_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
    
    public void updateEvent(Event event) throws SQLException {
        String sql = "UPDATE events SET title=?, event_date=?, event_time=?, description=?, event_type=?, importance_level=? "
                   + "WHERE event_id=?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, event.getTitle());
            pstmt.setDate(2, Date.valueOf(event.getDate()));
            pstmt.setString(3, event.getTime() != null ? event.getTime().toString() : null);
            pstmt.setString(4, event.getDescription());
            pstmt.setString(5, event.getType());
            pstmt.setInt(6, event.getImportanceLevel());
            pstmt.setInt(7, event.getId());
            
            pstmt.executeUpdate();
        }
    }
    
    public Event getEventById(int id) throws SQLException {
        String sql = "SELECT * FROM events WHERE event_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToEvent(rs);
            }
        }
        return null;
    }
    
    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("event_id"));
        event.setTitle(rs.getString("title"));
        event.setDate(rs.getDate("event_date").toLocalDate());
        
        String timeStr = rs.getString("event_time");
        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                if (timeStr.length() == 5) {
                    timeStr += ":00";
                }
                event.setTime(LocalTime.parse(timeStr));
            } catch (Exception e) {
                event.setTime(null);
            }
        }
        
        event.setDescription(rs.getString("description"));
        event.setType(rs.getString("event_type"));
        event.setImportanceLevel(rs.getInt("importance_level"));
        return event;
    }
}