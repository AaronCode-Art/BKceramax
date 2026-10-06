package com.ceramax.api.security;

import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Usuario;
import com.ceramax.api.repository.acceso.ClienteRepository;
import com.ceramax.api.repository.acceso.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CeramaxUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;

    public CeramaxUserDetailsService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findWithRolByEmail(username).orElse(null);
        if (usuario != null) {
            String role = usuario.getRol() != null ? usuario.getRol().getCodigo() : "USER";
            return User.withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
        }

        Cliente cliente = clienteRepository.findByEmail(username).orElse(null);
        if (cliente != null) {
            return User.withUsername(cliente.getEmail())
                .password(cliente.getPasswordHash() == null ? "" : cliente.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                .disabled(!Boolean.TRUE.equals(cliente.getActivo()))
                .build();
        }

        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}
