package dev.swote.interv.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.swote.interv.domain.company.entity.Company;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.service.company.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
public class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CompanyService companyService;

    private Company testCompany;
    private Position testPosition;
    private List<Company> testCompanies;
    private List<Position> testPositions;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testCompany = new Company();
        testCompany.setId(1);
        testCompany.setName("Test Company");
        testCompany.setIndustry("IT");
        testCompany.setDescription("A test company");

        Set<String> skills = new HashSet<>();
        skills.add("Java");
        skills.add("Spring");

        testPosition = new Position();
        testPosition.setId(1);
        testPosition.setCompany(testCompany);
        testPosition.setTitle("Software Engineer");
        testPosition.setDescription("SE position");
        testPosition.setRequiredSkills(skills);

        testCompanies = Arrays.asList(testCompany);
        testPositions = Arrays.asList(testPosition);
    }

    @Test
    @WithMockUser // Mock 사용자 인증 추가
    @DisplayName("Get all companies - success")
    void getAllCompanies_Success() throws Exception {
        when(companyService.getAllCompanies()).thenReturn(testCompanies);

        mockMvc.perform(get("/api/companies")
                        .with(csrf())) // CSRF 토큰 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Test Company")))
                .andExpect(jsonPath("$.data[0].industry", is("IT")));

        verify(companyService, times(1)).getAllCompanies();
    }

    @Test
    @WithMockUser
    @DisplayName("Search companies - success")
    void searchCompanies_Success() throws Exception {
        when(companyService.searchCompanies(anyString())).thenReturn(testCompanies);

        mockMvc.perform(get("/api/companies/search")
                        .param("keyword", "Test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Test Company")));

        verify(companyService, times(1)).searchCompanies("Test");
    }

    @Test
    @WithMockUser
    @DisplayName("Get company by ID - success")
    void getCompany_Success() throws Exception {
        when(companyService.getCompanyById(anyInt())).thenReturn(testCompany);

        mockMvc.perform(get("/api/companies/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.name", is("Test Company")))
                .andExpect(jsonPath("$.data.industry", is("IT")));

        verify(companyService, times(1)).getCompanyById(1);
    }

    @Test
    @WithMockUser
    @DisplayName("Create company - success")
    void createCompany_Success() throws Exception {
        when(companyService.createCompany(anyString(), anyString(), anyString()))
                .thenReturn(testCompany);

        CompanyController.CreateCompanyRequest request = new CompanyController.CreateCompanyRequest();
        request.setName("Test Company");
        request.setIndustry("IT");
        request.setDescription("A test company");

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.name", is("Test Company")));

        verify(companyService, times(1))
                .createCompany("Test Company", "IT", "A test company");
    }

    @Test
    @WithMockUser
    @DisplayName("Get company positions - success")
    void getCompanyPositions_Success() throws Exception {
        when(companyService.getCompanyPositions(anyInt())).thenReturn(testPositions);

        mockMvc.perform(get("/api/companies/1/positions")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", is("Software Engineer")));

        verify(companyService, times(1)).getCompanyPositions(1);
    }

    @Test
    @WithMockUser
    @DisplayName("Create position - success")
    void createPosition_Success() throws Exception {
        when(companyService.createPosition(anyInt(), anyString(), anyString(), anyList()))
                .thenReturn(testPosition);

        CompanyController.CreatePositionRequest request = new CompanyController.CreatePositionRequest();
        request.setTitle("Software Engineer");
        request.setDescription("SE position");
        List<String> skills = Arrays.asList("Java", "Spring");
        request.setRequiredSkills(skills);

        mockMvc.perform(post("/api/companies/1/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.title", is("Software Engineer")));

        verify(companyService, times(1))
                .createPosition(eq(1), eq("Software Engineer"), eq("SE position"), anyList());
    }
}