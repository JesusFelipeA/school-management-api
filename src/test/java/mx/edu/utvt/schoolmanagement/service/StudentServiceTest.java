package mx.edu.utvt.schoolmanagement.service;

import java.util.List;
import java.util.Optional;
import mx.edu.utvt.schoolmanagement.dto.request.CreateStudentRequest;
import mx.edu.utvt.schoolmanagement.dto.request.UpdateStudentRequest;
import mx.edu.utvt.schoolmanagement.dto.request.UpdateStudentStatusRequest;
import mx.edu.utvt.schoolmanagement.dto.response.PageResponse;
import mx.edu.utvt.schoolmanagement.dto.response.StudentResponse;
import mx.edu.utvt.schoolmanagement.entity.Student;
import mx.edu.utvt.schoolmanagement.exception.DuplicateStudentNumberException;
import mx.edu.utvt.schoolmanagement.exception.StudentNotFoundException;
import mx.edu.utvt.schoolmanagement.mapper.StudentMapper;
import mx.edu.utvt.schoolmanagement.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link StudentService} con JUnit 5 y Mockito.
 * El repositorio y el mapper se simulan; no se levanta contexto de Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private StudentMapper mapper;

    @InjectMocks
    private StudentService service;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("valida el número, persiste y devuelve la respuesta mapeada")
        void createsAndPersistsStudent() {
            CreateStudentRequest request = createRequest("STU-001");
            Student student = student("STU-001");
            StudentResponse expected = response(1L, "STU-001", true);
            when(repository.existsByStudentNumber("STU-001")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(student);
            when(repository.saveAndFlush(student)).thenReturn(student);
            when(mapper.toResponse(student)).thenReturn(expected);

            assertEquals(expected, service.create(request));

            InOrder order = inOrder(repository, mapper);
            order.verify(repository).existsByStudentNumber("STU-001");
            order.verify(mapper).toEntity(request);
            order.verify(repository).saveAndFlush(student);
            order.verify(mapper).toResponse(student);
            verifyNoMoreInteractions(repository, mapper);
        }

        @Test
        @DisplayName("rechaza un número de estudiante duplicado sin persistir")
        void rejectsDuplicateStudentNumberOnCreation() {
            CreateStudentRequest request = createRequest("STU-001");
            when(repository.existsByStudentNumber("STU-001")).thenReturn(true);

            DuplicateStudentNumberException exception = assertThrows(
                    DuplicateStudentNumberException.class, () -> service.create(request));

            assertEquals("Student number already exists: STU-001", exception.getMessage());
            verifyNoInteractions(mapper);
            verify(repository, never()).saveAndFlush(any(Student.class));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("devuelve el estudiante encontrado")
        void findsStudentById() {
            Student student = student("STU-001");
            StudentResponse expected = response(1L, "STU-001", true);
            when(repository.findById(1L)).thenReturn(Optional.of(student));
            when(mapper.toResponse(student)).thenReturn(expected);

            assertEquals(expected, service.findById(1L));
        }

        @Test
        @DisplayName("lanza StudentNotFoundException si no existe")
        void reportsMissingStudentById() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            StudentNotFoundException exception = assertThrows(
                    StudentNotFoundException.class, () -> service.findById(99L));

            assertEquals("Student not found: 99", exception.getMessage());
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("usa la página y el tamaño solicitados, ordenando por id")
        void returnsPagedStudentsUsingRequestedPageAndSize() {
            Student student = student("STU-001");
            StudentResponse expectedStudent = response(1L, "STU-001", true);
            when(repository.findAll(ArgumentMatchers.<Specification<Student>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(student), PageRequest.of(2, 5), 11));
            when(mapper.toResponse(student)).thenReturn(expectedStudent);

            PageResponse<StudentResponse> result = service.findAll(2, 5, true, "STU-001");

            assertEquals(List.of(expectedStudent), result.content());
            assertEquals(2, result.page());
            assertEquals(5, result.size());
            assertEquals(11, result.totalElements());
            assertEquals(3, result.totalPages());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findAll(ArgumentMatchers.<Specification<Student>>any(), pageable.capture());
            assertEquals(PageRequest.of(2, 5, Sort.by("id")), pageable.getValue());
        }

        @Test
        @DisplayName("devuelve una página vacía sin invocar al mapper")
        void returnsEmptyPageWhenNoStudentsMatch() {
            when(repository.findAll(ArgumentMatchers.<Specification<Student>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            PageResponse<StudentResponse> result = service.findAll(0, 20, null, null);

            assertTrue(result.content().isEmpty());
            assertEquals(0, result.page());
            assertEquals(20, result.size());
            assertEquals(0, result.totalElements());
            assertEquals(0, result.totalPages());
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("mapea cada elemento de la página en orden")
        void mapsEveryStudentOfThePage() {
            Student first = student("STU-001");
            Student second = student("STU-002");
            StudentResponse firstResponse = response(1L, "STU-001", true);
            StudentResponse secondResponse = response(2L, "STU-002", false);
            when(repository.findAll(ArgumentMatchers.<Specification<Student>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));
            when(mapper.toResponse(first)).thenReturn(firstResponse);
            when(mapper.toResponse(second)).thenReturn(secondResponse);

            PageResponse<StudentResponse> result = service.findAll(0, 20, null, "  ");

            assertEquals(List.of(firstResponse, secondResponse), result.content());
            assertEquals(2, result.totalElements());
            assertEquals(1, result.totalPages());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("actualiza los datos editables y persiste")
        void updatesStudentDetails() {
            Student student = student("STU-001");
            UpdateStudentRequest request = new UpdateStudentRequest(
                    "STU-002", "Ana Maria", "Lopez", "Diaz", "ana.maria@utvtol.edu.mx", null);
            StudentResponse expected = response(1L, "STU-002", true);
            when(repository.findById(1L)).thenReturn(Optional.of(student));
            when(repository.existsByStudentNumberAndIdNot("STU-002", 1L)).thenReturn(false);
            when(repository.saveAndFlush(student)).thenReturn(student);
            when(mapper.toResponse(student)).thenReturn(expected);

            assertEquals(expected, service.update(1L, request));

            InOrder order = inOrder(repository, mapper);
            order.verify(repository).findById(1L);
            order.verify(repository).existsByStudentNumberAndIdNot("STU-002", 1L);
            order.verify(mapper).update(student, request);
            order.verify(repository).saveAndFlush(student);
        }

        @Test
        @DisplayName("rechaza un número usado por otro estudiante")
        void rejectsStudentNumberUsedByAnotherStudentOnUpdate() {
            Student student = student("STU-001");
            UpdateStudentRequest request = new UpdateStudentRequest(
                    "STU-002", "Ana", "Lopez", null, "ana@utvtol.edu.mx", null);
            when(repository.findById(1L)).thenReturn(Optional.of(student));
            when(repository.existsByStudentNumberAndIdNot("STU-002", 1L)).thenReturn(true);

            assertThrows(DuplicateStudentNumberException.class, () -> service.update(1L, request));

            verify(mapper, never()).update(any(Student.class), any(UpdateStudentRequest.class));
            verify(repository, never()).saveAndFlush(any(Student.class));
        }

        @Test
        @DisplayName("lanza StudentNotFoundException antes de validar duplicados")
        void reportsMissingStudentOnUpdate() {
            UpdateStudentRequest request = new UpdateStudentRequest(
                    "STU-002", "Ana", "Lopez", null, "ana@utvtol.edu.mx", null);
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(StudentNotFoundException.class, () -> service.update(99L, request));

            verify(repository, never()).existsByStudentNumberAndIdNot(anyString(), anyLong());
            verify(repository, never()).saveAndFlush(any(Student.class));
            verifyNoInteractions(mapper);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("desactiva al estudiante y persiste")
        void updatesStudentActiveStatus() {
            Student student = student("STU-001");
            StudentResponse expected = response(1L, "STU-001", false);
            when(repository.findById(1L)).thenReturn(Optional.of(student));
            when(repository.saveAndFlush(student)).thenReturn(student);
            when(mapper.toResponse(student)).thenReturn(expected);

            assertEquals(expected, service.updateStatus(1L, new UpdateStudentStatusRequest(false)));

            assertFalse(student.isActive());
            verify(repository).saveAndFlush(student);
        }

        @Test
        @DisplayName("reactiva a un estudiante inactivo")
        void reactivatesInactiveStudent() {
            Student student = student("STU-001");
            student.setActive(false);
            StudentResponse expected = response(1L, "STU-001", true);
            when(repository.findById(1L)).thenReturn(Optional.of(student));
            when(repository.saveAndFlush(student)).thenReturn(student);
            when(mapper.toResponse(student)).thenReturn(expected);

            assertTrue(service.updateStatus(1L, new UpdateStudentStatusRequest(true)).active());
            assertTrue(student.isActive());
        }

        @Test
        @DisplayName("lanza StudentNotFoundException si no existe")
        void reportsMissingStudentOnStatusUpdate() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(StudentNotFoundException.class,
                    () -> service.updateStatus(99L, new UpdateStudentStatusRequest(false)));

            verify(repository, never()).saveAndFlush(any(Student.class));
            verifyNoInteractions(mapper);
        }
    }

    private CreateStudentRequest createRequest(String studentNumber) {
        return new CreateStudentRequest(studentNumber, "Ana", "Lopez", null, "ana@utvtol.edu.mx", null);
    }

    private Student student(String studentNumber) {
        Student student = new Student();
        student.setStudentNumber(studentNumber);
        student.setFirstName("Ana");
        student.setPaternalSurname("Lopez");
        student.setEmail("ana@utvtol.edu.mx");
        return student;
    }

    private StudentResponse response(Long id, String studentNumber, boolean active) {
        return new StudentResponse(id, studentNumber, "Ana", "Lopez", null, "ana@utvtol.edu.mx", null, active);
    }
}
