/*
 * (C) Copyright 2018-2019 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * This program is not suitable for any direct or indirect application in MILITARY industry
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.meveo.model.technicalservice.endpoint;

import org.meveo.model.technicalservice.InputMeveoProperty;

import javax.persistence.*;

/**
 * Configuration of an endpoint allowing to use a technical service.
 *
 * @author clement.bareth
 * @since 01.02.2019
 */
@Entity
@Table(name = "endpoint_path_parameter")
public class EndpointPathParameter {

    @EmbeddedId
    private EndpointParameter endpointParameter;

    /**
     * Position of the parameter the endpoint's path parameter list.
     * This column is used only for JPA to build list in right order.
     */
    @Column(name = "position", nullable = false)
    private int position;

    @PrePersist @PreUpdate
    private void prePersist(){
        position = endpointParameter.getEndpoint().getPathParameters().indexOf(this);
    }

    @Override
    public String toString() {
        return endpointParameter.toString();
    }

    public EndpointParameter getEndpointParameter() {
        return endpointParameter;
    }

    public void setEndpointParameter(EndpointParameter endpointParameter) {
        this.endpointParameter = endpointParameter;
    }

    public int getPosition() {
        return position;
    }

}