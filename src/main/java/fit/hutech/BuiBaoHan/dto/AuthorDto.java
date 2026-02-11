package fit.hutech.BuiBaoHan.dto;

import java.time.LocalDate;

import fit.hutech.BuiBaoHan.entities.Author;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AuthorDto(
        Long id,
        
        @NotBlank(message = "Tên tác giả không được để trống")
        @Size(max = 100, message = "Tên tác giả không quá 100 ký tự")
        String name,
        
        LocalDate birthDate,
        
        LocalDate deathDate,
        
        @Size(max = 2000, message = "Tiểu sử không quá 2000 ký tự")
        String biography,
        
        String avatar,
        
        @Size(max = 50, message = "Quốc tịch không quá 50 ký tự")
        String nationality,
        
        String website,
        
        Boolean isActive,
        
        Long bookCount
) {
    public static AuthorDto from(Author author) {
        return AuthorDto.builder()
                .id(author.getId())
                .name(author.getName())
                .birthDate(author.getBirthDate())
                .deathDate(author.getDeathDate())
                .biography(author.getBiography())
                .avatar(author.getAvatar())
                .nationality(author.getNationality())
                .website(author.getWebsite())
                .isActive(author.getIsActive())
                .bookCount((long) author.getBookCount())
                .build();
    }
    
    public Author toEntity() {
        return Author.builder()
                .id(id)
                .name(name)
                .birthDate(birthDate)
                .deathDate(deathDate)
                .biography(biography)
                .avatar(avatar)
                .nationality(nationality)
                .website(website)
                .isActive(!Boolean.FALSE.equals(isActive))
                .build();
    }
}
