package fit.hutech.BuiBaoHan.dto;

import java.time.LocalDateTime;

import fit.hutech.BuiBaoHan.constants.CardStatus;
import fit.hutech.BuiBaoHan.constants.CardType;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import lombok.Builder;

@Builder
public record LibraryCardDto(
        Long id,
        String cardNumber,
        String avatar,
        LocalDateTime issueDate,
        LocalDateTime expiryDate,
        CardType cardType,
        CardStatus status,
        String notes,
        Long userId,
        String userName,
        Long issuedByLibrarianId,
        String librarianName,
        Boolean isValid,
        Integer maxBooks,
        Integer maxDays
) {
    public static LibraryCardDto from(LibraryCard card) {
        return LibraryCardDto.builder()
                .id(card.getId())
                .cardNumber(card.getCardNumber())
                .avatar(card.getAvatar())
                .issueDate(card.getIssueDate())
                .expiryDate(card.getExpiryDate())
                .cardType(card.getCardType())
                .status(card.getStatus())
                .notes(card.getNotes())
                .userId(card.getUser() != null ? card.getUser().getId() : null)
                .userName(card.getUser() != null ? card.getUser().getUsername() : null)
                .issuedByLibrarianId(card.getIssuedByLibrarian() != null ? card.getIssuedByLibrarian().getId() : null)
                .librarianName(card.getIssuedByLibrarian() != null ? card.getIssuedByLibrarian().getUsername() : null)
                .isValid(card.isValid())
                .maxBooks(card.getMaxBooks())
                .maxDays(card.getMaxDays())
                .build();
    }
}
