package com.cg.service.esport.organizer;

import com.cg.domain.esport.dto.*;
import com.cg.domain.esport.entities.*;
import com.cg.exception.DataInputException;
import com.cg.exception.UnauthorizedException;
import com.cg.repository.esport.*;
import com.cg.service.email.EmailSender;
import com.cg.service.email.SendEmailThread;
import com.cg.service.esport.avartar.IAvartarService;
import com.cg.service.esport.jwt.JwtService;
import com.cg.service.esport.securitycode.ISecurityCodeService;
import com.cg.service.esport.teamJoinTour.ITeamJoinTourService;
import com.cg.service.esport.user.IUserService;
import com.cg.utils.AppUtils;
import com.cg.utils.driver.GoogleDriveConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrganzierServiceImp implements IOrganizerService{
    @Autowired
    private OrganizerRepository organizerRepository;
    @Autowired
    private OrganizerFilterRepository organizerFilterRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private IAvartarService avartarService;
    @Autowired
    private IUserService userService;

    @Autowired
    private ISecurityCodeService securityCodeService;
    @Autowired
    private AppUtils appUtils;

    @Autowired
    private SendEmailThread sendEmailThread;
    @Autowired
    private OtpRepository otpRepository;
    @Autowired
    private ITeamJoinTourService teamJoinTourService;

    @Override
    public List<Organizer> findAll() {

        return organizerRepository.findAll();
    }

    @Override
    public Organizer getById(Long id) {
        return organizerRepository.getById(id);
    }

    @Override
    public Optional<Organizer> findById(Long id) {
        return organizerRepository.findById(id);
    }


    @Override
    public Organizer save(Organizer organizer) {
        return organizerRepository.save(organizer);
    }

    @Override
    public void remove(Long id) {
        organizerRepository.deleteById(id);
    }


    @Override
    @Transactional
    public OrganizerResponseDTO createOrganizer(OrganizerRequestDTO organizerDTO) {
        List<Organizer> organizer = organizerRepository.findByEmail(organizerDTO.getEmail());
        if(organizer.size()==0){
            Role role = roleRepository.getById(5L);
            User user = new User()
                    .setUsername(organizerDTO.getName())
                    .setPassword(organizerDTO.getPassword())
                    .setRole(role);
            user = userService.save(user);
            SecurityCode securityCode = securityCodeService.save(new SecurityCode().setUser(user));
            user.setCodeSecurity(securityCode.getId());
            Organizer organizerResult = organizerRepository.save(organizerDTO.toOrganizer().setUser(user));
            Avartar avartar = new Avartar();
            avartar = avartarService.save(avartar);
            avartar.setOrganizer(organizerResult);
            return organizerResult.toOrganizerResponseDTO(avartar.toAvartarDTO());
        }else{
            throw new DataInputException("Email đã tồn tại");
        }
    }
    @Override
    @Transactional
    public OrganizerResSecurity updateAvartar(OrganizerAvartarDTO organizerAvartarDTO) {
        User user = userService.findByCodeSecurity(organizerAvartarDTO.getCode());
        try{
            if(Objects.equals(user.getId(), organizerAvartarDTO.getUserId())){
                Organizer organizer = organizerRepository.getByUser(user);
                Avartar avartar = avartarService.findByOrganizer(organizer);
                String fileIdDelete = avartar.getFileUrl();
                avartarService.save(avartar.setFileUrl(appUtils.uploadAvartar(organizerAvartarDTO.getAvartar(), avartar.getId())));
                if(fileIdDelete != null){
                    String fileId = fileIdDelete.split("&")[1].split("=")[1];
                    try {
                        appUtils.deleteFile(fileId);
                    } catch (Exception e) {
                        throw new DataInputException("FAIL");
                    }
                }
                return organizer.toOrganizerResSecurity(avartar.toAvartarDTO());
            }else{
                throw new DataInputException("Nhà tổ chức không tồn tại");
            }
        }catch (Exception e){
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }

    @Override
    @Transactional
    public OrganizerResSecurity updateOrganizerNoAvarTar(OrganizerUpdateDTO organizerDTO) {
        User user = userService.findByCodeSecurity(organizerDTO.getCode());
        try{
            if(Objects.equals(user.getId(), organizerDTO.getUserId()) && user.getId()!=null && organizerDTO.getUserId()!=null){
                Organizer organizer = organizerRepository.getByUser(user);

                List<Organizer> organizerCheckEmail = organizerRepository.findByEmail(organizerDTO.getEmail());
                if(organizerCheckEmail.size() > 0 && !Objects.equals(organizer.getEmail(), organizerCheckEmail.get(0).getEmail())){
                    throw new DataInputException("Email đã tồn tại");
                }

                organizer = organizerDTO.toOrganizer().setUser(user).setId(organizer.getId());
                organizer = organizerRepository.save(organizer);
                Avartar avartar = avartarService.findByOrganizer(organizer);
                return organizer.toOrganizerResSecurity(avartar.toAvartarDTO());
            }else{
                throw new DataInputException("Nhà tổ chức không tồn tại");
            }
        }catch (Exception e){
            System.out.println(e);
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }

    @Override
    public OrganizerResponseDTO findByUserId(Long id) {
        Optional<Organizer> organizerOpt = organizerRepository.findById(id);
        if(organizerOpt.isPresent()){
            Avartar avartar = avartarService.findByOrganizer(organizerOpt.get());
            return organizerOpt.get().toOrganizerResponseDTO(avartar.toAvartarDTO());
        }else{
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }

    @Override
    public Page<OrganizerResponseDTO> filter(OrganizerFilter organizerFilter, Pageable pageable) {
        return organizerFilterRepository.findAllByFilters(organizerFilter,pageable)
                .map(organizer -> {
                    Avartar avartar = avartarService.findByOrganizer(organizer);
                    return organizer.toOrganizerResponseDTO(avartar.toAvartarDTO());
                });
    }

    @Override
    public Page<OrganizerResSecurity> filterByAdmin(OrganizerFilter organizerFilter, Pageable pageable) {
        return organizerFilterRepository.findAllByFilters(organizerFilter,pageable)
                .map(organizer -> {
                    Avartar avartar = avartarService.findByOrganizer(organizer);
                    return organizer.toOrganizerResSecurity(avartar.toAvartarDTO());
                });
    }

    @Override
    public OrganizerResSecurity findByCodeSecurity(String code) {
        User user = userService.findByCodeSecurity(code);
        try{
            if(user != null){
                Organizer organizer = organizerRepository.getByUser(user);
                Avartar avartar = avartarService.findByOrganizer(organizer);
                return organizer.toOrganizerResSecurity(avartar.toAvartarDTO());
            }else{
                throw new DataInputException("Nhà tổ chức không tồn tại");
            }
        }catch (Exception e){
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }
    @Override
    public OrganizerResSecurity findByAdmin(Long id) {
        Optional<Organizer> organizerOpt = organizerRepository.findById(id);
        if(organizerOpt.isPresent()){
            Avartar avartar = avartarService.findByOrganizer(organizerOpt.get());
            return organizerOpt.get().toOrganizerResSecurity(avartar.toAvartarDTO());
        }else{
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }

    @Override
    @Transactional
    public void deleteOrganizer(Long id) {
        Optional<Organizer> organizerOpt = organizerRepository.findById(id);
        if(organizerOpt.isPresent()){
            Organizer organizer = organizerOpt.get();
            User user = organizer.getUser();
            Avartar avartar = avartarService.findByOrganizer(organizer);
            SecurityCode code = securityCodeService.findByUser(user);
            if(avartar.getFileUrl()==null && avartar.getFileUrl().equals("")){
                securityCodeService.remove(code.getId());
                avartarService.remove(avartar.getId());
                organizerRepository.delete(organizer);
                userService.remove(user.getId());
            }else{
                throw new DataInputException("Không thể xóa nhà tổ chức sau khi đã kích hoạt");
            }
        }else{
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }

    @Override
    public List<OrganizerResSecurity> findByDeleted(Boolean deleted) {
        return organizerRepository.getByDeleted(deleted)
                .stream()
                .map(organizer -> {
                    Avartar avartar = avartarService.findByOrganizer(organizer);
                    return organizer.toOrganizerResSecurity(avartar.toAvartarDTO());
                }).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void setDeleted(Boolean deleted, Long id) {
        Optional<Organizer> organizerOpt = organizerRepository.findById(id);
        if(organizerOpt.isPresent()){
            User user = organizerOpt.get().getUser();
            userService.save((User) user.setDeleted(deleted));
            organizerOpt.get().setDeleted(deleted);
            organizerRepository.save(organizerOpt.get());
            if(!deleted) sendEmailThread.start(user, organizerOpt.get().getEmail());
        }else{
            throw new DataInputException("Nhà tổ chức không tồn tại");
        }
    }

    @Override
    public void acceptJoinTour(TeamJoinTourDTO teamJoinTourDTO) {
        teamJoinTourService.acceptJoinTour(teamJoinTourDTO);
    }

    @Override
    public void rejectJoinTour(TeamJoinTourDTO teamJoinTourDTO) {
        teamJoinTourService.rejectJoinTour(teamJoinTourDTO);
    }

}
