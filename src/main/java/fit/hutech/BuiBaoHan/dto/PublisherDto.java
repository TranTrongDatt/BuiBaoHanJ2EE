package fit.hutech.BuiBaoHan.dto;

import fit.hutech.BuiBaoHan.entities.Publisher;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PublisherDto(
        Long id,
        
        @NotBlank(message = "Tên nhà xuất bản không được để trống")
        @Size(max = 100, message = "Tên NXB không quá 100 ký tự")
        String name,
        
        @Email(message = "Email không hợp lệ")
        String email,
        
        @Size(max = 15, message = "Số điện thoại không quá 15 ký tự")
        String phone,
        
        String address,
        
        String logo,
        
        String website,
        
        @Size(max = 1000, message = "Mô tả không quá 1000 ký tự")
        String description,
        
        Long bookCount
) {
    public static PublisherDto from(Publisher publisher) {
        return PublisherDto.builder()
                .id(publisher.getId())
                .name(publisher.getName())
                .email(publisher.getEmail())
                .phone(publisher.getPhone())
                .address(publisher.getAddress())
                .logo(publisher.getLogo())
                .website(publisher.getWebsite())
                .description(publisher.getDescription())
                .bookCount((long) publisher.getBooks().size())
                .build();
    }
    
    public Publisher toEntity() {
        return Publisher.builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .logo(logo)
                .website(website)
                .description(description)
                .build();
    }
}
