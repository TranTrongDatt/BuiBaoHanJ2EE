package fit.hutech.BuiBaoHan.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.ItemInvoice;
@Repository
public interface IItemInvoiceRepository extends
JpaRepository<ItemInvoice, Long>{
}
