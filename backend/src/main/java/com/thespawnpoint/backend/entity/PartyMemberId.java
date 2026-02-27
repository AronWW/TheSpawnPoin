package com.thespawnpoint.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartyMemberId implements Serializable {

    private static final long serialVersionUID = 6275480354243255222L;

    @Column(name = "party_request_id", nullable = false)
    private Long partyRequestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartyMemberId)) return false;
        PartyMemberId that = (PartyMemberId) o;
        return Objects.equals(partyRequestId, that.partyRequestId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partyRequestId, userId);
    }
}

