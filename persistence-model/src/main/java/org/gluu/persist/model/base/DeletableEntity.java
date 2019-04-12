package org.gluu.persist.model.base;

import org.gluu.persist.annotation.LdapAttribute;
import org.gluu.persist.annotation.LdapEntry;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 */
@Entry
public class DeletableEntity extends BaseEntry implements Deletable {

    @Attribute(name = "oxAuthExpiration")
    private Date expirationDate;
    @Attribute(name = "oxDeletable")
    private boolean deletable = true;

    @Override
    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean canDelete() {
        return canDelete(new Date());
    }

    public boolean canDelete(Date now) {
        return deletable && (expirationDate == null || expirationDate.before(now));
    }

    @Override
    public String toString() {
        return "DeletableEntity{" +
                "expirationDate=" + expirationDate +
                ", deletable=" + deletable +
                "} " + super.toString();
    }
}
