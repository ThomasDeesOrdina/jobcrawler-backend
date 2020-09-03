package nl.ordina.jobcrawler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nl.ordina.jobcrawler.controller.exception.VacancyURLMalformedException;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Vacancy {
    /* this class will be saved in a table called vacancy
     *   a linking table called vacancy_skills will contain vacancy ID's and their corresponding skill ID's
     * */

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;
    @NotNull
    private String vacancyURL;
    private String title;
    private String broker;
    private String vacancyNumber;
    private String locationString;
    private String hours;
    private String salary;
    private String postingDate;
    @Column(columnDefinition = "TEXT")
    private String about;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "vacancy_skills",
            joinColumns = @JoinColumn(name = "vacancy_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @JsonIgnore
    Set<Skill> skills;  //a set is a collection that has no duplicates

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    Location location;

    public boolean hasValidURL() {
        if (!this.vacancyURL.startsWith("http"))
            this.vacancyURL = "https://" + this.vacancyURL; //adding the protocol, if not present

        URL url;
        HttpURLConnection huc;
        int responseCode = 0;

        try {
            url = new URL(this.vacancyURL);
            huc = (HttpURLConnection) url.openConnection();
            /*
             * Added a user agent as huxley gives a 403 forbidden error
             * This user agent will make it as if we are making the request from a modern browser
             */

            huc.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            huc.setRequestMethod("HEAD");   // faster because it doesn't download the response body
            responseCode = huc.getResponseCode();

            return responseCode == 200; //returns true if the website has a 200 OK response

        } catch (IOException e) {
            throw new VacancyURLMalformedException(this.vacancyURL);
        }
    }

    @Override
    public String toString() {
        String newLine = "\n";
        StringBuilder returnValue = new StringBuilder();
        returnValue.append(vacancyURL.toString() + newLine);
        returnValue.append(title + newLine);
        returnValue.append(broker + newLine);
        returnValue.append(vacancyNumber + newLine);
        returnValue.append(locationString + newLine);
        returnValue.append(hours + newLine);
        returnValue.append(salary + newLine);
        returnValue.append(postingDate + newLine);
        returnValue.append(about + newLine);
        returnValue.append(skills.toString() + newLine + newLine);
        returnValue.append(location.getLocationName() + newLine);
        returnValue.append(location.getLocation_id() + newLine);
        returnValue.append(location.getLat() + newLine);
        returnValue.append(location.getLon() + newLine);
        returnValue.append("*****************************************");
        return returnValue.toString();
    }

/*    @Override
    public String toString() {
        return "Vacancy{" +
                "id=" + id +
                ", vacancyURL='" + vacancyURL + '\'' +
                ", title='" + title + '\'' +
                ", broker='" + broker + '\'' +
                ", vacancyNumber='" + vacancyNumber + '\'' +
                ", locationString='" + locationString + '\'' +
                ", hours='" + hours + '\'' +
                ", salary='" + salary + '\'' +
                ", postingDate='" + postingDate + '\'' +
                ", about='" + about + '\'' +
                ", skills=" + skills +
                ", location=" + location +
                '}';
    }*/
}
