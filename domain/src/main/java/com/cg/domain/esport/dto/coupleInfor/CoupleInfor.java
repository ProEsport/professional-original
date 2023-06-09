package com.cg.domain.esport.dto.coupleInfor;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Date;
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CoupleInfor {
    private Long teamAId;
    private Integer scoreA;
    private Long teamBId;
    private Integer scoreB;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date createAt;
}
