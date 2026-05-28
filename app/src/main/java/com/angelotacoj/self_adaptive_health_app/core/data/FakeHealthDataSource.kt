package com.angelotacoj.self_adaptive_health_app.core.data

import com.angelotacoj.self_adaptive_health_app.core.model.Appointment
import com.angelotacoj.self_adaptive_health_app.core.model.AccessCredentials
import com.angelotacoj.self_adaptive_health_app.core.model.ExperimentGroup
import com.angelotacoj.self_adaptive_health_app.core.model.FakeHealthDataSet
import com.angelotacoj.self_adaptive_health_app.core.model.ReminderTemplate
import com.angelotacoj.self_adaptive_health_app.core.model.WellBeingRecord

class FakeHealthDataSource {
    fun getDataSet(group: ExperimentGroup): FakeHealthDataSet {
        return when (group) {
            ExperimentGroup.GroupA -> setA
            ExperimentGroup.GroupB -> setB
        }
    }

    private val setA = FakeHealthDataSet(
        id = "Conjunto A",
        accessCredentials = AccessCredentials(
            userCode = "PACIENTE2391",
            simulatedPin = "928102"
        ),
        appointment = Appointment(
            title = "Chequeo general (Simulado)",
            date = "15 de junio",
            time = "10:30 AM",
            instruction = "Traiga su documento de identidad. (Dato ficticio)",
            professionalName = "Dr. Juan Pérez (Ficticio)",
            specialty = "Medicina general simulada",
            location = "Clínica Simulada Bienestar",
            preparation = "Ayuno de 8 horas ficticio",
            itemsToBring = "Documento de identidad",
            accessibilityNote = "Acceso en planta baja"
        ),
        appointmentOptions = listOf(
            Appointment("Revisión de medicamentos (Simulada)", "10 de junio", "2:00 PM", "Traiga la lista de medicamentos.", "Dra. Ana López (Ficticio)", "Farmacología simulada", "Centro Médico Ficticio", "Ninguna", "Lista de medicamentos", "Piso 2"),
            Appointment("Chequeo general (Simulado)", "15 de junio", "10:30 AM", "Traiga su documento de identidad. (Dato ficticio)", "Dr. Juan Pérez (Ficticio)", "Medicina general simulada", "Clínica Simulada Bienestar", "Ayuno de 8 horas ficticio", "Documento de identidad", "Acceso en planta baja"),
            Appointment("Evaluación de visión (Simulada)", "21 de junio", "11:15 AM", "Traiga sus lentes si los usa.", "Dr. Carlos Ruiz (Ficticio)", "Oftalmología simulada", "Hospital Simulado Central", "Pupilas dilatadas ficticias", "Lentes", "Piso 4, ascensor B")
        ),
        wellBeingRecord = WellBeingRecord(
            label = "Nivel de bienestar (Simulado)",
            value = 7
        ),
        reminder = ReminderTemplate(
            activity = "Recordatorio de vitamina ficticio",
            time = "8:00 AM",
            frequency = "Todos los días"
        )
    )

    private val setB = FakeHealthDataSet(
        id = "Conjunto B",
        accessCredentials = AccessCredentials(
            userCode = "PACIENTE02",
            simulatedPin = "5678"
        ),
        appointment = Appointment(
            title = "Control preventivo (Ficticio)",
            date = "18 de junio",
            time = "9:45 AM",
            instruction = "Llegue 10 minutos antes. (Dato simulado)",
            professionalName = "Dra. María Gómez (Ficticio)",
            specialty = "Medicina preventiva simulada",
            location = "Consultorio Simulado Norte",
            preparation = "Ninguna",
            itemsToBring = "Carnet de vacunas ficticio",
            accessibilityNote = "Rampa disponible"
        ),
        appointmentOptions = listOf(
            Appointment("Orientación nutricional (Ficticia)", "12 de junio", "4:30 PM", "Traiga un registro de comidas si lo tiene.", "Lic. Roberto Díaz (Ficticio)", "Nutrición simulada", "Clínica Simulada Sur", "Registro de comidas de 3 días", "Registro de comidas", "Planta baja"),
            Appointment("Control preventivo (Ficticio)", "18 de junio", "9:45 AM", "Llegue 10 minutos antes. (Dato simulado)", "Dra. María Gómez (Ficticio)", "Medicina preventiva simulada", "Consultorio Simulado Norte", "Ninguna", "Carnet de vacunas ficticio", "Rampa disponible"),
            Appointment("Revisión de resultados (Simulada)", "24 de junio", "8:20 AM", "Traiga su comprobante de cita.", "Dr. Luis Sosa (Ficticio)", "Laboratorio simulado", "Hospital Simulado Este", "Ninguna", "Comprobante de cita", "Piso 1")
        ),
        wellBeingRecord = WellBeingRecord(
            label = "Nivel de energía (Simulado)",
            value = 6
        ),
        reminder = ReminderTemplate(
            activity = "Recordatorio de hidratación ficticio",
            time = "7:30 PM",
            frequency = "De lunes a viernes"
        )
    )
}
