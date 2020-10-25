package equipmentManagementSystem.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Department{

    @Id
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
}
