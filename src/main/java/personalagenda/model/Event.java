package personalagenda.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Event {
    private int id;
    private String title;
    private LocalDate date;
    private LocalTime time;
    private String description;
    private String type;
    private int importanceLevel;
    
    public Event() {}
    
    public Event(String title, LocalDate date, LocalTime time, 
                 String description, String type, int importanceLevel) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.description = description;
        this.type = type;
        this.importanceLevel = importanceLevel;
    }
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getImportanceLevel() { return importanceLevel; }
    public void setImportanceLevel(int importanceLevel) { this.importanceLevel = importanceLevel; }
    
    public boolean isImportant() {
        return importanceLevel >= 4;
    }
    
    public String getCategoryColor() {
        if (type == null) return "#F0F2F5"; // Gris clair élégant par défaut
        
        // Nouvelle palette de couleurs pastel modernes avec une touche de violet (cohérent avec le CSS)
        switch (type.toLowerCase()) {
            case "travail":
            case "professionnel":
            case "bureau":
                return "#D5AAFF"; // Violet pastel (Indigo clair)
            case "personnel":
            case "perso":
                return "#C8F7F2"; // Turquoise pastel
            case "étude":
            case "etude":
            case "cours":
            case "école":
            case "ecole":
                return "#B8E6FF"; // Bleu ciel pastel
            case "santé":
            case "sante":
            case "médecin":
            case "medecin":
            case "rdv médical":
                return "#C8F5C5"; // Vert menthe pastel
            case "famille":
                return "#FFDDE5"; // Rose pâle pastel
            case "loisirs":
            case "sport":
            case "détente":
            case "detente":
                return "#FFF3CD"; // Jaune doux pastel
            case "courses":
            case "achats":
                return "#FFDDB5"; // Pêche pastel
            default:
                return "#EDE7F6"; // Lavande très clair
        }
    }
    
    public String getTextColor() {
        if (type == null) return "#2D3436"; // Gris foncé élégant par défaut
        
        // Les couleurs pastel nécessitent un texte foncé pour garantir un bon contraste et la lisibilité
        switch (type.toLowerCase()) {
            case "travail":
            case "professionnel":
            case "bureau":
                return "#4A148C"; // Violet très foncé
            case "personnel":
            case "perso":
                return "#004D40"; // Teal foncé
            case "étude":
            case "etude":
            case "cours":
            case "école":
            case "ecole":
                return "#01579B"; // Bleu foncé
            case "santé":
            case "sante":
            case "médecin":
            case "medecin":
            case "rdv médical":
                return "#1B5E20"; // Vert foncé
            case "famille":
                return "#880E4F"; // Rose foncé
            case "loisirs":
            case "sport":
            case "détente":
            case "detente":
                return "#E65100"; // Orange foncé
            case "courses":
            case "achats":
                return "#BF360C"; // Brun rougeâtre foncé
            default:
                return "#311B92"; // Indigo foncé
        }
    }
    
    // Méthode ajoutée pour obtenir la couleur de la bordure gauche (Design moderne type "Tag")
    public String getAccentColor() {
        if (type == null) return "#B2BEC3";
        
        switch (type.toLowerCase()) {
            case "travail": case "professionnel": case "bureau": return "#6C5CE7";
            case "personnel": case "perso": return "#00B894";
            case "étude": case "etude": case "cours": case "école": case "ecole": return "#0984E3";
            case "santé": case "sante": case "médecin": case "medecin": return "#00B16A";
            case "famille": return "#E84393";
            case "loisirs": case "sport": case "détente": case "detente": return "#FDCB6E";
            case "courses": case "achats": return "#E17055";
            default: return "#A29BFE";
        }
    }
    
    @Override
    public String toString() {
        return title + " - " + date + " (" + type + ")";
    }
}